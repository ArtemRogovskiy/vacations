package com.netcracker.vacations.controller;

import com.netcracker.vacations.converter.DepartmentConverter;
import com.netcracker.vacations.dto.DepartmentDTO;
import com.netcracker.vacations.service.DepartmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
public class DepartmentsController {

    private final DepartmentService departmentService;

    public DepartmentsController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<DepartmentDTO> departments() {
        return departmentService.getDepartments().stream().map(DepartmentConverter::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public DepartmentDTO getDepartment(@PathVariable("id") Integer id) {

        return DepartmentConverter.toDTO(departmentService.getDepartment(id));
    }

    @PostMapping("/addDepartment")
    public DepartmentDTO addDepartment(@RequestBody DepartmentDTO departmentDTO) {
        return departmentService.addDepartment(departmentDTO);
    }

    @PutMapping("/{id}")
    public DepartmentDTO updateDepartment(
            @PathVariable("id") Integer id,
            @RequestBody DepartmentDTO departmentDTO
    ) {
        return departmentService.updateDepartment(id, departmentDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteDepartment(@PathVariable("id") Integer id) {
        departmentService.deleteDepartment(id);
    }
}
