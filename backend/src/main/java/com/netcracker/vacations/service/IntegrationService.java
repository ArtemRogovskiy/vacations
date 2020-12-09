package com.netcracker.vacations.service;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.netcracker.vacations.domain.RequestEntity;
import com.netcracker.vacations.domain.UserEntity;
import com.netcracker.vacations.repository.GoogleCredentialRepository;
import com.netcracker.vacations.repository.RequestRepository;
import com.netcracker.vacations.repository.UserRepository;
import com.netcracker.vacations.security.MyUserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Service
@Transactional
public class IntegrationService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final GoogleCredentialRepository googleCredentialRepository;

    public IntegrationService(RequestRepository requestRepository,
                              UserRepository userRepository,
                              GoogleCredentialRepository googleCredentialRepository) {

        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.googleCredentialRepository = googleCredentialRepository;
    }

    @Value("${google-client-client-id}")
    private String clientId;

    @Value("${google-client-client-secret}")
    private String clientSecret;

    @Value("${google-client-redirectUri}")
    private String redirectURI;

    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/calendar.events");
    private static final String APPLICATION_NAME = "Absence Management-app";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CALENDAR_ID = "primary";
    private static HttpTransport httpTransport;

    private GoogleAuthorizationCodeFlow flow;
    private String login;


    public void cancelIntegration() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserEntity currentUser = ((MyUserPrincipal) authentication.getPrincipal()).getUser();
        currentUser.setIntegrated(false);
        userRepository.save(currentUser);
    }

    public void insertEventsAfterApproval(List<Integer> requests) throws Exception {
        for (Integer id : requests) {
            RequestEntity requestEntity = requestRepository.findByRequestsId(id).get(0);
            checkUserConsentAndInsertEvent(requestEntity);
        }
    }

    public void insertEventWithoutConfirm(RequestEntity requestEntity) throws Exception {
        checkUserConsentAndInsertEvent(requestEntity);
    }

    private void checkUserConsentAndInsertEvent(RequestEntity requestEntity) throws Exception {
        Credential credential = getStoredCredential(requestEntity.getUser().getLogin());
        if (credential != null && requestEntity.getUser().getIntegrated()) {
            Calendar service = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            Event event = service.events().insert(CALENDAR_ID, makeEvent(requestEntity)).execute();
            requestEntity.setGoogleId(event.getId());
            requestRepository.save(requestEntity);
        }
    }

    public void insertEvents(String code) throws Exception {
        Calendar service;
        List<RequestEntity> requests = requestRepository.findAllByUserLogin(login);
        try {
            service = getCalendarService(code);
            service.events().list(CALENDAR_ID).execute();
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException | com.google.api.client.auth.oauth2.TokenResponseException e2) {
            TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectURI).execute();
            service = new Calendar.Builder(httpTransport, JSON_FACTORY, flow.createAndStoreCredential(response, login))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
        for (RequestEntity entity : requests) {
            if (entity.getStatus().getName().equals("Accepted")) {
                if (entity.getGoogleId() == null) {
                    Event event = service.events().insert(CALENDAR_ID, makeEvent(entity)).execute();
                    entity.setGoogleId(event.getId());
                    requestRepository.save(entity);
                } else if ("cancelled".equals(service.events().get(CALENDAR_ID, entity.getGoogleId()).execute().getStatus())) {
                    Event event = service.events().insert(CALENDAR_ID, makeEvent(entity)).execute();
                    entity.setGoogleId(event.getId());
                    requestRepository.save(entity);
                }
            }
        }
    }

    private Event makeEvent(RequestEntity requestEntity) {
        Event event = new Event()
                .setSummary(requestEntity.getTypeOfRequest().getName())
                .setDescription(requestEntity.getDescription());

        DateTime startDateTime = new DateTime(requestEntity.getBeginning().toString());
        EventDateTime start = new EventDateTime()
                .setDate(startDateTime);
        event.setStart(start);

        DateTime endDateTime = new DateTime(requestEntity.getEnding().plusDays(1).toString());
        EventDateTime end = new EventDateTime()
                .setDate(endDateTime);
        event.setEnd(end);

        EventReminder[] reminderOverrides = new EventReminder[]{
                new EventReminder().setMethod("email").setMinutes(24 * 60),
                new EventReminder().setMethod("popup").setMinutes(10),
        };
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminderOverrides));
        event.setReminders(reminders);
        event.setColorId("3");
        return event;
    }

    private Calendar getCalendarService(String code) throws Exception {
        return new Calendar.Builder(httpTransport, JSON_FACTORY, getCredential(code))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Credential getCredential(String code) throws Exception {
        UserEntity currentUser = userRepository.findByLogin(login).get(0);
        if (!currentUser.getIntegrated()) {
            currentUser.setIntegrated(true);
            userRepository.save(currentUser);
        }
        if (flow.loadCredential(login) != null) {
            return flow.loadCredential(login);
        }
        TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectURI).execute();
        return flow.createAndStoreCredential(response, login);
    }

    private Credential getStoredCredential(String login) throws Exception {
        getRedirectURL(login);
        return flow.loadCredential(login);
    }

    public String getRedirectURL(String login) throws Exception {
        this.login = login;
        if (flow == null) {
            GoogleClientSecrets.Details web = new GoogleClientSecrets.Details();
            web.setClientId(clientId);
            web.setClientSecret(clientSecret);
            GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setWeb(web);
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            // Build flow and trigger user authorization request.
            flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new MySqlDataStoreFactory(googleCredentialRepository))
                    .setAccessType("offline")
                    .build();
        }
        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectURI);
        return authorizationUrl.build();
    }

}
