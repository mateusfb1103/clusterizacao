package com.example.clusterizacao.service;

import com.example.clusterizacao.model.Pessoa;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PessoaService {

    private final Map<Long, Pessoa> pessoas = new HashMap<>();
    private final Map<String, Integer> profissaoMap = new HashMap<>();
    private int profissaoIndex = 0;
    private long nextId = 1;
    private static final double LIMIAR_DISTANCIA = 30.0;

    public PessoaService() {
        adicionarPessoa(new Pessoa());
        adicionarPessoa(new Pessoa());
        adicionarPessoa(new Pessoa());
        adicionarPessoa(new Pessoa());
        adicionarPessoa(new Pessoa());
    }

    public Pessoa adicionarPessoa(Pessoa pessoa) {
        if (pessoa.getId() == null) {
            pessoa.setId(nextId++);
        }

        if (!pessoa.isCentroide()) {
            Pessoa centroideProximo = encontrarCentroideMaisProximo(pessoa);
            if (centroideProximo != null) {
                pessoa.setClusterId(centroideProximo.getId());
            } else {
                pessoa.setClusterId(pessoa.getId());
                pessoa.setCentroide(true);
            }
        } else {
            pessoa.setClusterId(pessoa.getId());
        }

        pessoas.put(pessoa.getId(), pessoa);

        if (!pessoas.isEmpty()) {
            atualizarCentroide(pessoa.getClusterId());
            reorganizarClusters();
            analisarDispersao();
        }

        return pessoa;
    }

    public Pessoa atualizarPessoa(Long id, Pessoa novaPessoa) {
        if (pessoas.containsKey(id)) {
            novaPessoa.setId(id);
            Pessoa pessoaExistente = pessoas.get(id);
            if (novaPessoa.getClusterId() == null) {
                novaPessoa.setClusterId(pessoaExistente.getClusterId());
            }
            if (!novaPessoa.isCentroide() && pessoaExistente.isCentroide()) {
                novaPessoa.setCentroide(true);
            }
            pessoas.put(id, novaPessoa);
            atualizarCentroide(novaPessoa.getClusterId());
            reorganizarClusters();
            analisarDispersao();
            return novaPessoa;
        }
        return null;
    }

    public boolean removerPessoa(Long id) {
        boolean removed = pessoas.remove(id) != null;
        if (removed && !pessoas.isEmpty()) {
            reorganizarClusters();
            analisarDispersao();
        }
        return removed;
    }

    public List<Pessoa> listarPessoas() {
        return new ArrayList<>(pessoas.values());
    }

    public Pessoa obterPorId(Long id) {
        return pessoas.get(id);
    }

    private Pessoa obterPessoaNumerica(Pessoa pessoa) {
        int profissaoCode = profissaoMap.computeIfAbsent(
                pessoa.getProfissao() == null ? "Desconhecido" : pessoa.getProfissao(),
                key -> profissaoIndex++
        );

        Pessoa numericPessoa = new Pessoa();
        numericPessoa.setIdade(pessoa.getIdade());
        numericPessoa.setSalario(pessoa.getSalario());
        numericPessoa.setEscolaridade(pessoa.getEscolaridade());
        numericPessoa.setProfissao(String.valueOf(profissaoCode));

        return numericPessoa;
    }

    private double calcularDistancia(Pessoa p1, Pessoa p2) {
        Pessoa np1 = obterPessoaNumerica(p1);
        Pessoa np2 = obterPessoaNumerica(p2);

        double idade = np1.getIdade() - np2.getIdade();
        double salario = np1.getSalario() - np2.getSalario();
        double escolaridade = np1.getEscolaridade() - np2.getEscolaridade();

        double profissaoVal1 = 0;
        double profissaoVal2 = 0;

        try {
            if (np1.getProfissao() != null) {
                profissaoVal1 = Double.parseDouble(np1.getProfissao());
            }
            if (np2.getProfissao() != null) {
                profissaoVal2 = Double.parseDouble(np2.getProfissao());
            }
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter profissão para número: " + e.getMessage());
        }
        double profissao = profissaoVal1 - profissaoVal2;

        return Math.sqrt(idade * idade + salario * salario + escolaridade * escolaridade + profissao * profissao);
    }

    private Pessoa encontrarCentroideMaisProximo(Pessoa pessoa) {
        return pessoas.values().stream()
                .filter(Pessoa::isCentroide)
                .filter(c -> c.getId() != null)
                .min(Comparator.comparingDouble(c -> calcularDistancia(pessoa, c)))
                .orElse(null);
    }

    public Pessoa calcularCentroideVirtual(Long clusterId) {
        List<Pessoa> membros = pessoas.values().stream()
                .filter(p -> p.getClusterId() != null && clusterId.equals(p.getClusterId()) && !p.isCentroide())
                .toList();

        if (membros.isEmpty()) return null;

        double mediaIdade = membros.stream().mapToInt(Pessoa::getIdade).average().orElse(0);
        double mediaSalario = membros.stream().mapToDouble(Pessoa::getSalario).average().orElse(0);
        double mediaEscolaridade = membros.stream().mapToInt(Pessoa::getEscolaridade).average().orElse(0);

        Pessoa centroideVirtual = new Pessoa();
        centroideVirtual.setIdade((int) mediaIdade);
        centroideVirtual.setSalario(mediaSalario);
        centroideVirtual.setEscolaridade((int) mediaEscolaridade);
        centroideVirtual.setCentroide(true);
        return centroideVirtual;
    }

    public void atualizarCentroide(Long clusterId) {
        if (clusterId == null) return;

        Pessoa novoCentroide = calcularCentroideVirtual(clusterId);
        if (novoCentroide == null) {
            pessoas.values().removeIf(p -> p.isCentroide() && p.getId().equals(clusterId));
            return;
        }

        pessoas.values().removeIf(p -> p.isCentroide() && clusterId.equals(p.getId()));

        novoCentroide.setId(clusterId);
        novoCentroide.setClusterId(clusterId);
        pessoas.put(clusterId, novoCentroide);
    }

    public void reorganizarClusters() {
        if (pessoas.isEmpty()) return;

        for (Pessoa pessoa : new ArrayList<>(pessoas.values())) {
            if (!pessoa.isCentroide()) {
                Pessoa novoCentroide = encontrarCentroideMaisProximo(pessoa);
                if (novoCentroide != null && !novoCentroide.getId().equals(pessoa.getClusterId())) {
                    pessoa.setClusterId(novoCentroide.getId());
                }
            }
        }

        Set<Long> clusterIds = pessoas.values().stream()
                .filter(Pessoa::isCentroide)
                .map(Pessoa::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long clusterId : clusterIds) {
            atualizarCentroide(clusterId);
        }
    }

    public void analisarDispersao() {
        if (pessoas.isEmpty()) return;

        List<Pessoa> candidatos = new ArrayList<>();

        for (Pessoa pessoa : new ArrayList<>(pessoas.values())) {
            if (!pessoa.isCentroide()) {
                Pessoa centroideAtual = pessoas.get(pessoa.getClusterId());
                if (centroideAtual == null) {
                    continue;
                }
                double distAtual = calcularDistancia(pessoa, centroideAtual);

                Pessoa centroideMaisProximo = encontrarCentroideMaisProximo(pessoa);
                if (centroideMaisProximo == null) {
                    continue;
                }
                double distMaisProx = calcularDistancia(pessoa, centroideMaisProximo);

                if (distAtual > LIMIAR_DISTANCIA && !centroideMaisProximo.equals(centroideAtual)) {
                    candidatos.add(pessoa);
                }
            }
        }

        for (Pessoa p : candidatos) {
            Pessoa novoCentroide = new Pessoa();
            novoCentroide.setId(nextId++);
            novoCentroide.setCentroide(true);
            novoCentroide.setClusterId(novoCentroide.getId());

            pessoas.put(novoCentroide.getId(), novoCentroide);
            p.setClusterId(novoCentroide.getId());
        }

        if (!candidatos.isEmpty()) {
            reorganizarClusters();
        }
    }
}
