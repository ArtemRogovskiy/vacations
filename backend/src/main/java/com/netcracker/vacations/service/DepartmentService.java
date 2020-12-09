package com.netcracker.vacations.service;

import com.netcracker.vacations.domain.DepartmentEntity;
import com.netcracker.vacations.dto.DepartmentDTO;
import com.netcracker.vacations.repository.DepartmentRepository;
import com.netcracker.vacations.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentService(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    public List<DepartmentEntity> getDepartments() {
        List<DepartmentEntity> response = new ArrayList<>();
        for (DepartmentEntity entity : departmentRepository.findAll()) {
            response.add(entity);
        }
        return response;
    }

    public DepartmentEntity getDepartment(Integer id) {
        return departmentRepository.findById(id).get();
    }

    public DepartmentDTO addDepartment(DepartmentDTO departmentDTO) {
        DepartmentEntity entity = toEntity(departmentDTO);
        departmentRepository.save(entity);
        return departmentDTO;
    }

    public void deleteDepartment(Integer id) {
        departmentRepository.deleteById(id);
    }

    public DepartmentDTO updateDepartment(Integer id, DepartmentDTO departmentDTO) {
        DepartmentEntity departmentEntity = departmentRepository.findByDepartmentsId(id).get(0);
        BeanUtils.copyProperties(toEntity(departmentDTO), departmentEntity, "departmentsId");
        departmentRepository.save(departmentEntity);
        return departmentDTO;
    }

    private DepartmentEntity toEntity(DepartmentDTO departmentDTO) {
        DepartmentEntity departmentEntity = new DepartmentEntity();
        if (departmentDTO.getDirectorId() == null) {
            departmentEntity.setDirector(null);
        } else {
            departmentEntity.setDirector(userRepository.findByUsersId(departmentDTO.getDirectorId()).get(0));
        }
        departmentEntity.setDepartmentsId(null);
        departmentEntity.setName(departmentDTO.getName());
        return departmentEntity;
    }

}
