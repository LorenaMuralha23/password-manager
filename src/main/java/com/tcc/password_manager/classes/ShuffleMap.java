package com.tcc.password_manager.classes;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ShuffleMap {
    int originalIndex; 
    int value;
    
    public ShuffleMap() {
    }
    
    public ShuffleMap(int originalIndex, int value) {
        this.originalIndex = originalIndex;
        this.value = value;
    }
    
}
