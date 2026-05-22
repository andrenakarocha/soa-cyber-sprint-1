package com.henryplatform.telemetry.security;

// SEC: roles bem definidos garantem que cada ator só acessa o que lhe é permitido (princípio do menor privilégio)
public enum UserRole {
    CUSTOMER,         // dono do veículo — lê seus próprios dados
    MECHANIC,         // técnico da concessionária — lê e atualiza leituras processadas
    DEALER_MANAGER,   // gestor da concessionária — acesso total à concessionária
    FORD_ADMIN        // Ford corporativo — acesso total a todos os dados
}
