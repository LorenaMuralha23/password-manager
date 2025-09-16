package com.tcc.password_manager.interfaces;

import com.tcc.password_manager.classes.ShuffleMap;
import com.tcc.password_manager.dto.FragmentedData;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.hibernate.mapping.Collection;

/**
 *
 * @author Lolo
 */
public class FragmentationService {

    private static final SecureRandom secureRandom = new SecureRandom();

    private ArrayList<Integer> originalCutPoints = new ArrayList<>();
    //7GtXmccDUSZI/7BXAy03/Q==H -> 25
    //[4, 6, 19 (25-6)]

    //7G tXmcc D USZI /7BXAy 03/Q==H
    //0 - 8    8 - 16   16 - 8
    //sortear quantas partes quebrar <- armazenar -> 4
    //quebrar a string no número de fragmentos necessários (nesse caso, 24/3 -> arredonda para baixo, se for o caso -> 6
    //sorteia o cutpoint (n-1) -> ) <- armazenar ordem original [2, 5, 1, 4, 6]
    //embaralha os fragmentos -> [] <- armazenar nova ordem [4, 6, 2, 5, 1]
    //unir a senha nessa nova ordem USZI 03/Q <-> ==H tXmcc /7BXAy 7G
    //sortear um número para cortar a senha novamente - 8
    // retorna os dois fragmentos
    public FragmentedData fragmentPassword(String password) {
        int cutNumber = getRandomCutPoint(password.length());
        double fragmentsNumber = Math.floor(password.length() - cutNumber);
        ArrayList<Integer> cutPoints = new ArrayList<>();

        ArrayList<Integer> numeros = new ArrayList<>(Arrays.asList(3, 2, 9, 5, 8));

        for (int i = 0; i < fragmentsNumber; i++) {
            cutPoints.add(getRandomCutPoint((int) fragmentsNumber));
        }

        ArrayList<String> passwordFragments = breakPassword(password, cutPoints);
        ArrayList<ShuffleMap> shuffledCutpoints = shuffleCutPoints(this.originalCutPoints);
        String reorganizedPassword = organizePassword(this.originalCutPoints, shuffledCutpoints, passwordFragments);

        int newCutPoint = getRandomCutPoint(reorganizedPassword.length());
        String fragment1 = reorganizedPassword.substring(0, newCutPoint);
        String frament2 = reorganizedPassword.substring(newCutPoint, reorganizedPassword.length());
        FragmentedData fragmentationMap = new FragmentedData(fragment1, frament2, shuffledCutpoints);
        
        return fragmentationMap;
    }

    public ArrayList<String> breakPassword(String password, ArrayList<Integer> cutPoints) {
        ArrayList<String> passwordFragments = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < cutPoints.size(); i++) {
            if ((start + cutPoints.get(i)) > password.length()) {
                passwordFragments.add(password.substring(start, password.length()));
                break;
            } else {
                passwordFragments.add(password.substring(start, start + cutPoints.get(i)));
                start += cutPoints.get(i);
            }
            this.originalCutPoints.add(cutPoints.get(i));
        }

        return passwordFragments;
    }

    public ArrayList<ShuffleMap> shuffleCutPoints(ArrayList<Integer> cutPoints) {
        ArrayList<ShuffleMap> shuffleList = new ArrayList<>();

        for (int i = 0; i < cutPoints.size(); i++) {
            shuffleList.add(new ShuffleMap(i, cutPoints.get(i)));
        }

        shuffleList.sort((a, b) -> Integer.compare(a.getValue(), b.getValue()));

        return shuffleList;
    }

    public int getRandomCutPoint(int max) {
        if (max <= 1) {
            throw new IllegalArgumentException("O tamanho deve ser maior que 1 para permitir corte.");
        }
        // Garante um valor entre 1 e max-1
        return 1 + secureRandom.nextInt(max - 1);
    }

    public String organizePassword(ArrayList<Integer> originalCutpoints, ArrayList<ShuffleMap> newCutPoints, ArrayList<String> passwordFragments) {
        ArrayList<String> orderedFragments = new ArrayList<>(Collections.nCopies(passwordFragments.size(), ""));

        for (int i = 0; i < newCutPoints.size(); i++) {
            ShuffleMap map = newCutPoints.get(i);
            orderedFragments.set(map.getOriginalIndex(), passwordFragments.get(i));
        }

        StringBuilder password = new StringBuilder();
        for (String fragment : orderedFragments) {
            password.append(fragment);
        }

        return password.toString();
    }

//    public FragmentedData breakPassword(String password){ 
//        int cutPoint = getRandomCutPoint(password.length()); 
//        String fragment1 = password.substring(0, cutPoint);
//        String fragment2 = password.substring(cutPoint, password.length());
//        
//        FragmentedData fragmentedData = new FragmentedData();
//        fragmentedData.setCutPoint(cutPoint);
//        fragmentedData.setFragment1(fragment1);
//        fragmentedData.setFragment2(fragment2);
//        
//        return fragmentedData;
//    }
}
