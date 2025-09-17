package com.tcc.password_manager.classes;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author Lolo
 */
@Getter
@Setter
@ToString
public class ShuffleMap {
    int originalIndex; 
    int value;

    public ShuffleMap(int originalIndex, int value) {
        this.originalIndex = originalIndex;
        this.value = value;
    }
    
    
}
