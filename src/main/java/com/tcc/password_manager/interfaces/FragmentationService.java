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

    public FragmentedData fragmentPassword(String password) {
        int cutNumber = getRandomCutPoint(password.length());
        double fragmentsNumber = Math.floor(password.length() - cutNumber);
        ArrayList<Integer> cutPoints = new ArrayList<>();

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

        String original = joinPassword(reorganizedPassword, shuffledCutpoints);

        return fragmentationMap;
    }

    public ArrayList<String> breakPassword(String password, ArrayList<Integer> cutPoints) {
        ArrayList<String> passwordFragments = new ArrayList<>();
        this.originalCutPoints.clear(); // garante que começa vazio

        int start = 0;

        while (start < password.length()) {
            // sorteia um tamanho de corte válido para o que resta da senha
            int remaining = password.length() - start;
            int cut = getRandomCutPoint(remaining);

            // se o corte ultrapassar o fim, ajusta
            int end = Math.min(start + cut, password.length());

            // adiciona o fragmento cortado
            passwordFragments.add(password.substring(start, end));

            // salva o tamanho do corte
            this.originalCutPoints.add(end - start);

            // avança
            start = end;
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
            max += 2;
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

    public String joinPassword(String shuffledPassword, ArrayList<ShuffleMap> newCutPoints) {
        ArrayList<String> fragments = new ArrayList<>();

        int start = 0;
        for (ShuffleMap map : newCutPoints) {
            int end = start + map.getValue();
            if (end > shuffledPassword.length()) {
                end = shuffledPassword.length(); // segurança
            }
            fragments.add(shuffledPassword.substring(start, end));
            start = end;
        }

        ArrayList<String> orderedFragments = new ArrayList<>(Collections.nCopies(fragments.size(), ""));
        for (int i = 0; i < newCutPoints.size(); i++) {
            ShuffleMap map = newCutPoints.get(i);
            orderedFragments.set(map.getOriginalIndex(), fragments.get(i));
        }

        StringBuilder password = new StringBuilder();
        for (String fragment : orderedFragments) {
            password.append(fragment);
        }

        String originalPassoword = password.toString();

        return password.toString();
    }

}
