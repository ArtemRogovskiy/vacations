package com.netcracker.vacations.domain;

import com.netcracker.vacations.domain.enums.Statuses;
import com.netcracker.vacations.domain.exceptions.BeginningAfterEndingException;
import com.netcracker.vacations.domain.exceptions.EndingBeforeDateException;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="requests")
public class RequestEntity {
    @Id
    @GeneratedValue(generator = "increment")

    @Column(name = "requests_id")
        private Integer requestsId;
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "users_id", nullable = false)
        private UserEntity usersId;

    @Column(name = "beginning_ov_vacation", nullable=false)
    @Temporal(TemporalType.DATE)
        private Date beginning;

    @Column(name = "enging_ov_vacation", nullable=false)
    @Temporal(TemporalType.DATE)
        private Date ending;
    @ManyToOne (cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "type_of_request_id", referencedColumnName = "type_of_requests_id")
        private RequestTypeEntity typeOfRequest;

    @Column(name = "status", nullable=false)
        private String status;

    @Column(name = "description")
        private String description;

    public RequestEntity() {
    }

    public RequestEntity(UserEntity usersId, Date beginning, Date ending, RequestTypeEntity typeOfRequest, Statuses status, String description) throws EndingBeforeDateException{
        this.usersId = usersId;
        this.typeOfRequest = typeOfRequest;
        this.status = status.name;
        this.description = description;

        if (ending.before(beginning)) {
            throw new EndingBeforeDateException();
        } else{
            this.beginning = beginning;
            this.ending = ending;
        }
    }

    public RequestEntity(UserEntity usersId, Date beginning, Date ending, RequestTypeEntity typeOfRequest, Statuses status) throws EndingBeforeDateException{
        this.usersId = usersId;
        this.typeOfRequest = typeOfRequest;
        this.status = status.name;

        if (ending.before(beginning)) {
            throw new EndingBeforeDateException();
        } else{
            this.beginning = beginning;
            this.ending = ending;
        }
    }

    public Integer getRequestsId() {
        return requestsId;
    }

    public void setRequestsId(Integer requestsId) {
        this.requestsId = requestsId;
    }

    public UserEntity getUsersId() {
        return usersId;
    }

    public void setUsersId(UserEntity usersId) {
        this.usersId = usersId;
    }

    public Date getBeginning() {
        return beginning;
    }

    public void setBeginning(Date beginning) throws BeginningAfterEndingException{
        if ((beginning.after(ending))&&(ending!=null)){
            throw new BeginningAfterEndingException();
        } else{
        this.beginning = beginning;}
    }

    public Date getEnding() {
        return ending;
    }

    public void setEnding(Date ending) throws EndingBeforeDateException{
        if ((ending.before(beginning))&&(beginning!=null)){
            throw new EndingBeforeDateException();
        } else{
            this.ending = ending;}
    }

    public RequestTypeEntity getTypeOfRequest() {
        return typeOfRequest;
    }

    public void setTypeOfRequest(RequestTypeEntity typeOfRequest) {
        this.typeOfRequest = typeOfRequest;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(Statuses status) {
        this.status = status.name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
