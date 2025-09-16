package com.tcc.password_manager.dto;

import com.tcc.password_manager.classes.ShuffleMap;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Lolo
 */
@Getter
@Setter
public class FragmentedData {
    private String fragment1;
    private String fragment2;
    private ArrayList<ShuffleMap> cutPointsMap;

    public FragmentedData(String fragment1, String fragment2, ArrayList<ShuffleMap> cutPointsMap) {
        this.fragment1 = fragment1;
        this.fragment2 = fragment2;
        this.cutPointsMap = cutPointsMap;
    }
    
}
