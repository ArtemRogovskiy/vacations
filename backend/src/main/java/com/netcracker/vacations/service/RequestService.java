package com.netcracker.vacations.service;

import com.netcracker.vacations.domain.RequestEntity;
import com.netcracker.vacations.domain.RequestTypeEntity;
import com.netcracker.vacations.domain.enums.Status;
import com.netcracker.vacations.dto.RequestDTO;
import com.netcracker.vacations.repository.RequestRepository;
import com.netcracker.vacations.repository.RequestTypeRepository;
import com.netcracker.vacations.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class RequestService {

    private RequestRepository requestRepository;
    private RequestTypeRepository requestTypeRepository;
    private UserRepository userRepository;

    @Autowired
    public RequestService(RequestRepository requestRepository, RequestTypeRepository requestTypeRepository, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.requestTypeRepository = requestTypeRepository;
        this.userRepository = userRepository;
    }

    public void saveRequest(RequestDTO request) {
        RequestTypeEntity type = requestTypeRepository.findByName(request.getType()).get(0);
        Status status = Status.CONSIDER;
        if (!type.getNeedApproval()) {
            status = Status.ACCEPTED;
        }
        RequestEntity requestEntity = new RequestEntity(
                userRepository.findByLogin(request.getUsername()).get(0),
                request.getStart(),
                request.getEnd(),
                type,
                status
        );
        requestEntity.setDescription(request.getDescription());
        requestRepository.save(requestEntity);
    }


    public void updateRequest(Status status, List<Integer> requests) {
        //TODO add logic to decrement aount of vacant days left
        for (Integer id : requests) {
            RequestEntity entity = requestRepository.findById(id).get();
            entity.setStatus(status);
            requestRepository.save(entity);
        }
    }

    public List<RequestDTO> getActiveRequests() {
        List<RequestDTO> response = new ArrayList<>();
        for (RequestEntity entity : requestRepository.findAll()) {
            if (entity.getStatus().equals(Status.CONSIDER.getName())) {
                response.add(toDTO(entity));
            }
        }
        return response;
    }

    public List<RequestDTO> getResolvedRequests() {
        List<RequestDTO> response = new ArrayList<>();
        for (RequestEntity entity : requestRepository.findAll()) {
            if (!entity.getTypeOfRequest().getNeedApproval()
                || !entity.getStatus().equals(Status.CONSIDER.getName())) {
                response.add(toDTO(entity));
            }
        }
        return response;
    }

    private RequestDTO toDTO(RequestEntity entity) {
        RequestDTO requestDTO = new RequestDTO();
        requestDTO.setId(entity.getRequestsId());
        requestDTO.setName(entity.getUser().getName() + " " + entity.getUser().getFamilyName());
        requestDTO.setDescription(entity.getDescription());
        requestDTO.setStart(entity.getBeginning());
        requestDTO.setEnd(entity.getEnding());
        requestDTO.setType(entity.getTypeOfRequest().getName());
        requestDTO.setStatus(entity.getStatus());
        return requestDTO;
    }

    public List<List<String>> getRequests() {
        List<List<String>> response = new ArrayList<>();
        for (RequestEntity entity : requestRepository.findAll()) {
            if (entity.getStatus().equals(Status.ACCEPTED.getName()))
                response.add(toTimelineDTO(entity));
        }
        return response;
    }

    private List<String> toTimelineDTO(RequestEntity entity) {
        List<String> res = new ArrayList<>();
        res.add(entity.getUser().getName() + " " + entity.getUser().getFamilyName());
        res.add(entity.getTypeOfRequest().getName());
        res.add(entity.getBeginning().toString());
        res.add(entity.getEnding().toString());
        return res;
    }
}
