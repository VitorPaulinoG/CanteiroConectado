package br.com.canteiroconectado.repository.usuario;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import br.com.canteiroconectado.dominio.usuario.Usuario;
import br.com.canteiroconectado.dominio.usuario.UsuarioSituacao;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByAdministradorTrue();

    Optional<Usuario> findFirstByAdministradorTrueOrderByCriadoEmAsc();

    @Query("""
        select new br.com.canteiroconectado.dominio.usuario.UsuarioSituacao(
            u.ativo, u.perfil, u.administrador, u.canteiroId, u.precisaTrocarSenha)
        from Usuario u where u.id = :id
        """)
    Optional<UsuarioSituacao> buscarSituacao(UUID id);
}
