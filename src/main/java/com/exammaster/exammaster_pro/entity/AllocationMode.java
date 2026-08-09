package com.exammaster.exammaster_pro.entity;

public enum AllocationMode {
    STRICT,    // Branch-mixed benches only (original mode)
    FREE,      // Any mix allowed
    FLEXIBLE   // Smart auto-balancing based on branch distribution
}
