package br.com.canteiroconectado.usuario;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import br.com.canteiroconectado.comum.Paginacao;

import jakarta.persistence.criteria.Predicate;

final class UsuarioEspecificacoes {

    private UsuarioEspecificacoes() {
    }

    static Specification<Usuario> filtrar(String busca, Perfil perfil, Boolean ativo) {
        String termo = Paginacao.termoDeBusca(busca);
        return (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (termo != null) {
                String padrao = "%" + termo + "%";
                condicoes.add(cb.or(
                    cb.like(cb.lower(raiz.get("nome")), padrao, '\\'),
                    cb.like(cb.lower(raiz.get("email")), padrao, '\\')));
            }
            if (perfil != null) {
                condicoes.add(cb.equal(raiz.get("perfil"), perfil));
            }
            if (ativo != null) {
                condicoes.add(cb.equal(raiz.get("ativo"), ativo));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }
}
