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
            log.debug("Iniciando fragmentacao. Comprimento total da entrada: {} caracteres", password.length());

            int cutNumber = getRandomCutPoint(password.length());
            double fragmentsNumber = Math.floor(password.length() - cutNumber);
            ArrayList<Integer> cutPoints = new ArrayList<>();

            for (int i = 0; i < fragmentsNumber; i++) {
                cutPoints.add(getRandomCutPoint((int) fragmentsNumber));
            }
            log.debug("Pontos de corte gerados: {}", cutPoints);

            ArrayList<String> passwordFragments = breakPassword(password, cutPoints);
            ArrayList<ShuffleMap> shuffledCutpoints = shuffleCutPoints(this.originalCutPoints);

            // --- Diagnóstico detalhado ---
            log.debug("OriginalCutPoints: {}", this.originalCutPoints);
            log.debug("Mapa embaralhado gerado (antes da reorganizacao):");
            for (ShuffleMap sm : shuffledCutpoints) {
                log.debug("originalIndex={} | value={}", sm.getOriginalIndex(), sm.getValue());
            }

            String reorganizedPassword = organizePassword(this.originalCutPoints, shuffledCutpoints, passwordFragments);

            // --- Diagnóstico do texto reorganizado ---
            log.debug("Senha reorganizada (pre-fragmentacao final): {}",
                    reorganizedPassword.substring(0, Math.min(60, reorganizedPassword.length())));

            int newCutPoint = getRandomCutPoint(reorganizedPassword.length());
            String fragment1 = reorganizedPassword.substring(0, newCutPoint);
            String fragment2 = reorganizedPassword.substring(newCutPoint, reorganizedPassword.length());
            log.info("Fragmentacao concluida. Fragmento 1: {} chars, Fragmento 2: {} chars",
                    fragment1.length(), fragment2.length());
            log.debug("Novo ponto de corte final: {}", newCutPoint);

            // --- Mapa final completo ---
            log.debug("Mapa de reconstrucao final:");
            for (ShuffleMap sm : shuffledCutpoints) {
                log.debug("originalIndex={} | value={}", sm.getOriginalIndex(), sm.getValue());
            }

            FragmentedData fragmentationMap = new FragmentedData(fragment1, fragment2, shuffledCutpoints);
            return fragmentationMap;
        } catch (Exception e) {
            log.error("Falha durante o processo de fragmentacao: {}", e.getMessage());
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

        log.debug("Senha dividida em {} fragmentos. Padrao de cortes: {}",
                passwordFragments.size(), this.originalCutPoints);

        // Diagnóstico de fragmentos
        for (int i = 0; i < passwordFragments.size(); i++) {
            String frag = passwordFragments.get(i);
            log.debug("Fragment[{}] -> \"{}...\" ({} chars)", i,
                    frag.substring(0, Math.min(10, frag.length())), frag.length());
        }

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

        log.debug("Antes do sort(value):");
        for (ShuffleMap sm : shuffleList) {
            log.debug("originalIndex={} | value={}", sm.getOriginalIndex(), sm.getValue());
        }

        shuffleList.sort((a, b) -> Integer.compare(a.getValue(), b.getValue()));

        log.debug("Depois do sort(value):");
        for (ShuffleMap sm : shuffleList) {
            log.debug("originalIndex={} | value={}", sm.getOriginalIndex(), sm.getValue());
        }

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
        log.trace("Ponto de corte aleatorio gerado: {}", point);
        return point;
    }

    /**
     * Reorganiza os fragmentos de acordo com o novo mapa embaralhado.
     */
    public String organizePassword(ArrayList<Integer> originalCutpoints, ArrayList<ShuffleMap> newCutPoints, ArrayList<String> passwordFragments) {
        ArrayList<String> orderedFragments = new ArrayList<>(Collections.nCopies(passwordFragments.size(), ""));

        log.debug("Iniciando reorganizacao. newCutPoints.size={} | passwordFragments.size={}",
                newCutPoints.size(), passwordFragments.size());

        for (int i = 0; i < newCutPoints.size(); i++) {
            ShuffleMap map = newCutPoints.get(i);
            log.debug("i={} | map(originalIndex={}, value={}) | fragment[i]={}...",
                    i, map.getOriginalIndex(), map.getValue(),
                    passwordFragments.get(i).substring(0, Math.min(8, passwordFragments.get(i).length())));
            String frag = passwordFragments.get(map.getOriginalIndex());
            orderedFragments.set(i, frag);
        }

        StringBuilder password = new StringBuilder();
        for (String fragment : orderedFragments) {
            password.append(fragment);
        }

        log.debug("Senha reorganizada apos embaralhamento. Novo comprimento: {}", password.length());
        return password.toString();
    }

    /**
     * Recompõe a senha original a partir dos fragmentos e do mapa de
     * embaralhamento.
     */
    public String joinPassword(String shuffledPassword, ArrayList<ShuffleMap> newCutPoints) {
        LogTimer timer = LogTimer.start("Join password operation");

        try {
            log.debug("Iniciando recomposicao. shuffledPassword.length={} | mapSize={}",
                    shuffledPassword.length(), newCutPoints.size());

            ArrayList<String> fragments = new ArrayList<>();

            int start = 0;
            for (ShuffleMap map : newCutPoints) {
                int end = start + map.getValue();
                if (end > shuffledPassword.length()) {
                    end = shuffledPassword.length();
                }
                fragments.add(shuffledPassword.substring(start, end));
                log.debug("Fragment join[{}] => len={} ({}..{})", map.getOriginalIndex(), map.getValue(), start, end);
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

            log.info("Recomposicao concluida. Tamanho final: {} caracteres.", password.length());
            return password.toString();
        } catch (Exception e) {
            log.error("Falha ao recompor a senha: {}", e.getMessage());
            throw new RuntimeException("Erro ao recompor a senha", e);
        } finally {
            timer.stopAndLog(log);
        }
    }

}
