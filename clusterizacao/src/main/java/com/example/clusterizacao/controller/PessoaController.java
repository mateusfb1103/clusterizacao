package com.example.clusterizacao.controller;

import com.example.clusterizacao.model.Pessoa;
import com.example.clusterizacao.service.PessoaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pessoas")
public class PessoaController {

    @Autowired
    private PessoaService pessoaService;

    @PostMapping
    public Pessoa adicionar(@RequestBody Pessoa pessoa) {
        return pessoaService.adicionarPessoa(pessoa);
    }

    @GetMapping
    public List<Pessoa> listar() {
        return pessoaService.listarPessoas();
    }

    @GetMapping("/{id}")
    public Pessoa buscar(@PathVariable Long id) {
        return pessoaService.obterPorId(id);
    }

    @PutMapping("/{id}")
    public Pessoa atualizar(@PathVariable Long id, @RequestBody Pessoa pessoa) {
        return pessoaService.atualizarPessoa(id, pessoa);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        pessoaService.removerPessoa(id);
    }
}
