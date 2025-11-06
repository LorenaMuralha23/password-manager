package com.tcc.password_manager.service;

import com.tcc.password_manager.classes.ShuffleMap;
import com.tcc.password_manager.dto.FragmentedData;
import com.tcc.password_manager.util.LogTimer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FragmentationService {

    private static final Logger log = LoggerFactory.getLogger(FragmentationService.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    private ArrayList<Integer> originalCutPoints = new ArrayList<>();

    /**
     * Fragmenta uma senha em dois fragmentos, embaralhando sua estrutura
     * interna e gerando um mapa de reconstrução.
     *
     * @param password senha ou ciphertext a ser fragmentado
     * @return objeto FragmentedData contendo fragmentos e mapa de reorganização
     */
    public FragmentedData fragmentPassword(String password) {
        LogTimer timer = LogTimer.start("Fragmentation operation");

        try {
            log.debug("Iniciando fragmentação. Comprimento total da entrada: {} caracteres", password.length());

            int cutNumber = getRandomCutPoint(password.length());
            double fragmentsNumber = Math.floor(password.length() - cutNumber);
            ArrayList<Integer> cutPoints = new ArrayList<>();

            for (int i = 0; i < fragmentsNumber; i++) {
                cutPoints.add(getRandomCutPoint((int) fragmentsNumber));
            }
            log.debug("Pontos de corte gerados: {}", cutPoints);

            ArrayList<String> passwordFragments = breakPassword(password, cutPoints);
            ArrayList<ShuffleMap> shuffledCutpoints = shuffleCutPoints(this.originalCutPoints);
            String reorganizedPassword = organizePassword(this.originalCutPoints, shuffledCutpoints, passwordFragments);

            int newCutPoint = getRandomCutPoint(reorganizedPassword.length());
            String fragment1 = reorganizedPassword.substring(0, newCutPoint);
            String fragment2 = reorganizedPassword.substring(newCutPoint, reorganizedPassword.length());
            log.info("Fragmentação concluída. Fragmento 1: {} chars, Fragmento 2: {} chars",
                    fragment1.length(), fragment2.length());
            log.debug("Novo ponto de corte final: {}", newCutPoint);

            FragmentedData fragmentationMap = new FragmentedData(fragment1, fragment2, shuffledCutpoints);
            return fragmentationMap;
        } catch (Exception e) {
            log.error("Falha durante o processo de fragmentação: {}", e.getMessage());
            throw new RuntimeException("Erro ao fragmentar dados: " + e.getMessage(), e);
        } finally {
            timer.stopAndLog(log);
        }
    }

    /**
     * Divide uma string em fragmentos com base em pontos de corte aleatórios.
     */
    public ArrayList<String> breakPassword(String password, ArrayList<Integer> cutPoints) {
        ArrayList<String> passwordFragments = new ArrayList<>();
        this.originalCutPoints.clear(); // garante que começa vazio

        int start = 0;

        while (start < password.length()) {
            int remaining = password.length() - start;
            int cut = getRandomCutPoint(remaining);
            int end = Math.min(start + cut, password.length());

            passwordFragments.add(password.substring(start, end));

            this.originalCutPoints.add(end - start);

            start = end;
        }

        log.debug("Senha dividida em {} fragmentos. Padrão de cortes: {}",
                passwordFragments.size(), this.originalCutPoints);
        return passwordFragments;
    }

    /**
     * Cria um mapa de embaralhamento dos pontos de corte, preservando índices
     * originais.
     */
    public ArrayList<ShuffleMap> shuffleCutPoints(ArrayList<Integer> cutPoints) {
        ArrayList<ShuffleMap> shuffleList = new ArrayList<>();

        for (int i = 0; i < cutPoints.size(); i++) {
            shuffleList.add(new ShuffleMap(i, cutPoints.get(i)));
        }

        shuffleList.sort((a, b) -> Integer.compare(a.getValue(), b.getValue()));
        log.debug("Mapa de embaralhamento criado com {} elementos.", shuffleList.size());

        return shuffleList;
    }

    /**
     * Retorna um ponto de corte aleatório entre 1 e max-1.
     */
    public int getRandomCutPoint(int max) {
        if (max <= 1) {
            max = 2;
        }

        int point = 1 + secureRandom.nextInt(max - 1);
        log.trace("Ponto de corte aleatório gerado: {}", point);
        return point;
    }

    /**
     * Reorganiza os fragmentos de acordo com o novo mapa embaralhado.
     */
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

        log.debug("Senha reorganizada após embaralhamento. Novo comprimento: {}", password.length());
        return password.toString();
    }

    /**
     * Recompõe a senha original a partir dos fragmentos e do mapa de
     * embaralhamento.
     */
    public String joinPassword(String shuffledPassword, ArrayList<ShuffleMap> newCutPoints) {
        LogTimer timer = LogTimer.start("Join password operation");

        try {
            ArrayList<String> fragments = new ArrayList<>();

            int start = 0;
            for (ShuffleMap map : newCutPoints) {
                int end = start + map.getValue();
                if (end > shuffledPassword.length()) {
                    end = shuffledPassword.length();
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

            log.info("Recomposição concluída. Tamanho final: {} caracteres.", password.length());
            return password.toString();
        } catch (Exception e) {
            log.error("Falha ao recompor a senha: {}", e.getMessage());
            throw new RuntimeException("Erro ao recompor a senha", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

}
