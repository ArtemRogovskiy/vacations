package com.netcracker.vacations.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.AbstractDataStore;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.netcracker.vacations.domain.GoogleCredential;
import com.netcracker.vacations.repository.GoogleCredentialRepository;

import java.io.IOException;
import java.util.*;

public class MySqlCredentialsDataStore extends AbstractDataStore<StoredCredential> {

    private final GoogleCredentialRepository googleCredentialRepository;

    /**
     * @param dataStoreFactory data store factory
     * @param id               data store ID
     */
    MySqlCredentialsDataStore(DataStoreFactory dataStoreFactory, String id,
                              GoogleCredentialRepository googleCredentialRepository) {
        super(dataStoreFactory, id);
        this.googleCredentialRepository = googleCredentialRepository;
    }


    @Override
    public Set<String> keySet() {
        Iterable<GoogleCredential> list = googleCredentialRepository.findAll();
        Set<String> result = new HashSet<>();
        for (GoogleCredential credential : list) {
            result.add(credential.getValueId());
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Collection<StoredCredential> values() throws IOException {
        Iterable<GoogleCredential> list = googleCredentialRepository.findAll();
        List<StoredCredential> result = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (GoogleCredential credential : list) {
//            result.add(mapper.readValue(credential.getValue(), new TypeReference<V>(){}));
            result.add(mapper.readValue(credential.getValue(), StoredCredential.class));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public StoredCredential get(String key) throws IOException {
        if (key == null) {
            return null;
        }
        List<GoogleCredential> credList = googleCredentialRepository.findAllByValueId(key);
        if (credList.isEmpty()) return null;
        GoogleCredential googleCredential = credList.get(0);
        String encodedString = googleCredential.getValue();
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        String jsonStr = new String(decodedBytes);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonStr, StoredCredential.class);
    }

    @Override
    public DataStore<StoredCredential> set(String key, StoredCredential value) throws IOException {
        GoogleCredential googleCredential = new GoogleCredential();
        googleCredential.setValueId(key);
        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = mapper.writeValueAsString(value);
        String encodedString = Base64.getEncoder().encodeToString(jsonStr.getBytes());
        googleCredential.setValue(encodedString);
        googleCredentialRepository.save(googleCredential);
        return this;
    }

    @Override
    public com.google.api.client.util.store.DataStore<StoredCredential> clear() {
        googleCredentialRepository.deleteAll();
        return this;
    }

    @Override
    public com.google.api.client.util.store.DataStore<StoredCredential> delete(String key) {
        googleCredentialRepository.deleteById(key);
        return this;
    }
}
