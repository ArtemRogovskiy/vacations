package com.netcracker.vacations.service;

import com.netcracker.vacations.domain.RequestEntity;
import com.netcracker.vacations.domain.TeamEntity;
import com.netcracker.vacations.domain.UserEntity;
import com.netcracker.vacations.domain.enums.Status;
import com.netcracker.vacations.repository.*;
import com.netcracker.vacations.security.SecurityExpressionMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Service
public class CalendarService {
    private final UserRepository userRepo;
    private final TeamRepository teamRepo;
    private final DepartmentRepository depRepo;
    private final RequestRepository reqRepo;
    SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);

    @Autowired
    public CalendarService(UserRepository userRepo, TeamRepository teamRepo, DepartmentRepository depRepo, RequestRepository reqRepo) {
        this.userRepo = userRepo;
        this.teamRepo = teamRepo;
        this.depRepo = depRepo;
        this.reqRepo = reqRepo;
    }

    public List<List<String>> getVacationsPerDay(String mode, String purpose) {
        List<Date> dates;
        ArrayList<RequestEntity> teamReqs;
        List<UserEntity> teamUsers;

        String name = SecurityExpressionMethods.currentUserLogin();
        UserEntity user = userRepo.findByLogin(name).get(0);
        List<TeamEntity> teams = new ArrayList<>();
        List<List<String>> occupiedAll = new ArrayList<>();
        List<List<String>> busyAll = new ArrayList<>();


        try {
            int year = Calendar.getInstance().get(Calendar.YEAR);
            Date BeginDate = formatter.parse("01 January " + year);
            Date EndDate = formatter.parse("31 December " + year);

            dates = getDatesBetween(BeginDate, EndDate);

            if (user.getRole().getName().equals("Employee") || purpose.equals("Send")) {
                teams.add(user.getTeam());
            } else if (user.getRole().getName().equals("Manager")) {
                teams = teamRepo.findAllByManager(user);
            } else if (user.getRole().getName().equals("Director")) {
                teams = teamRepo.findAllByDepartment(depRepo.findByDirector(user).get(0));
            } else if (user.getRole().getName().equals("Administrator")) {
                teams = teamRepo.findAll(); //GET RID OF THIS METHOD LATER
            }
            for (TeamEntity team : teams) {

                if (!(team == null)) {
                    List<String> occupied = new ArrayList<>();
                    List<String> busy = new ArrayList<>();
                    occupied.add(team.getName());
                    busy.add(team.getName());

                    teamReqs = new ArrayList<>();
                    teamUsers = userRepo.findAllByTeam(team);


                    Calendar cal1 = Calendar.getInstance();
                    Calendar cal2 = Calendar.getInstance();
                    Calendar cal3 = Calendar.getInstance();


                    int quota = team.getQuota();

                    for (UserEntity users : teamUsers) {
                        List<RequestEntity> userReqs = reqRepo.findAllByUser(users);
                        for (RequestEntity req : userReqs) {
                            if (req.getStatus().equals(Status.ACCEPTED) && (req.getTypeOfRequest().getInfluenceOnVr())) {
                                teamReqs.add(req);
                            }
                        }
                    }

                    for (Date date : dates) {
                        int counter = 0;
                        for (RequestEntity req : teamReqs) {
                            Date begin = convertToDateViaSqlDate(req.getBeginning());
                            Date end = convertToDateViaSqlDate(req.getEnding());
                            cal1.setTime(date);
                            cal2.setTime(begin);
                            cal3.setTime(end);
                            boolean sameDayBegin = isSameDay(cal1, cal2);
                            boolean sameDayEnd = isSameDay(cal1, cal3);
                            if ((((begin).before(date)) || sameDayBegin) && ((((end).after(date)) || sameDayEnd))) {
                                counter++;
                            }
                        }
                        if (counter >= quota) {
                            occupied.add(formatter.format(date));
                        } else if (counter >= quota / 2) {
                            busy.add(formatter.format(date));
                        }
                    }

                    occupiedAll.add(occupied);
                    busyAll.add(busy);
                }
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        if (mode.equals("Occupied")) {
            return occupiedAll;
        }
        if (mode.equals("Busy")) {
            return busyAll;
        }
        return null;
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR);
    }

    private Date convertToDateViaSqlDate(LocalDate dateToConvert) {
        return java.sql.Date.valueOf(dateToConvert);
    }

    private List<Date> getDatesBetween(Date startDate, Date endDate) {
        List<Date> datesInRange = new ArrayList<>();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(startDate);
        DateFormat format = new SimpleDateFormat("MM/dd/yy");

        Calendar endCalendar = new GregorianCalendar();
        endCalendar.setTime(endDate);
        try {
            while (calendar.before(endCalendar)) {
                String dateString = format.format(calendar.getTime());
                Date result = format.parse(dateString);
                datesInRange.add(result);
                calendar.add(Calendar.DATE, 1);
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return datesInRange;
    }

    public Map<String, List<String>> getVacations() {
        String name = SecurityExpressionMethods.currentUserLogin();
        UserEntity user = userRepo.findByLogin(name).get(0);
        Map<String, List<String>> vacCollection = new HashMap<>();

        List<RequestEntity> reqs = reqRepo.findAllByUser(user);
        List<RequestEntity> accepted = new ArrayList<>();
        List<RequestEntity> declined = new ArrayList<>();
        List<RequestEntity> consider = new ArrayList<>();

        for (RequestEntity req : reqs) {
            if (req.getStatus().equals(Status.ACCEPTED)) {
                accepted.add(req);
            }
            if (req.getStatus().equals(Status.DECLINED)) {
                declined.add(req);
            }
            if (req.getStatus().equals(Status.CONSIDER)) {
                consider.add(req);
            }
        }

        vacCollection.put(Status.ACCEPTED.getName(), collectToString(accepted));
        vacCollection.put(Status.DECLINED.getName(), collectToString(declined));
        vacCollection.put(Status.CONSIDER.getName(), collectToString(consider));

        return vacCollection;
    }

    private List<String> collectToString(List<RequestEntity> reqs) {
        List<RequestEntity> business = new ArrayList<>();
        List<RequestEntity> child = new ArrayList<>();
        List<RequestEntity> vacation = new ArrayList<>();
        List<RequestEntity> sick = new ArrayList<>();
        List<RequestEntity> remote = new ArrayList<>();

        List<String> dates = new ArrayList<>();

        String begin;
        String end;

        for (RequestEntity req : reqs) {
            collectRequests(business, child, vacation, sick, remote, req);
        }
        for (RequestEntity req : business) {
            begin = req.getBeginning().toString();
            end = req.getEnding().toString();
            dates.add("BU//" + begin + "//" + end);
        }
        for (RequestEntity req : child) {
            begin = req.getBeginning().toString();
            end = req.getEnding().toString();
            dates.add("CH//" + begin + "//" + end);
        }
        for (RequestEntity req : vacation) {
            begin = req.getBeginning().toString();
            end = req.getEnding().toString();
            dates.add("VA//" + begin + "//" + end);
        }
        for (RequestEntity req : sick) {
            begin = req.getBeginning().toString();
            end = req.getEnding().toString();
            dates.add("SI//" + begin + "//" + end);
        }
        for (RequestEntity req : remote) {
            begin = req.getBeginning().toString();
            end = req.getEnding().toString();
            dates.add("RE//" + begin + "//" + end);
        }
        return dates;
    }

    private void collectRequests(List<RequestEntity> business, List<RequestEntity> child, List<RequestEntity> vacation, List<RequestEntity> sick, List<RequestEntity> remote, RequestEntity req) {
        switch (req.getTypeOfRequest().getName()) {
            case "Business trip":
                business.add(req);
                break;
            case "Child care":
                child.add(req);
                break;
            case "Vacation":
                vacation.add(req);
                break;
            case "Remote work":
                remote.add(req);
                break;
            case "Sick leave":
                sick.add(req);
                break;
        }
    }

}

