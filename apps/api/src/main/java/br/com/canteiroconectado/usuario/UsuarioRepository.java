package br.com.canteiroconectado.usuario;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByAdministradorTrue();

    @Query("""
        select new br.com.canteiroconectado.usuario.SituacaoDoUsuario(
            u.ativo, u.perfil, u.administrador, u.canteiroId, u.precisaTrocarSenha)
        from Usuario u where u.id = :id
        """)
    Optional<SituacaoDoUsuario> buscarSituacao(UUID id);
}
