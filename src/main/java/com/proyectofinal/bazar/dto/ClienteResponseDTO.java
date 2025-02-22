package com.proyectofinal.bazar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ClienteResponseDTO {

    private String nombre;
    private String apellido;
    private String dni;
    private String domicilio;
}
