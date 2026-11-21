package br.com.canteiroconectado.support;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.canteiroconectado.canteiro.Canteiro;
import br.com.canteiroconectado.canteiro.CanteiroRepository;
import br.com.canteiroconectado.security.JwtService;
import br.com.canteiroconectado.usuario.Perfil;
import br.com.canteiroconectado.usuario.Usuario;
import br.com.canteiroconectado.usuario.UsuarioRepository;

/**
 * Cria canteiros e usuários gravados no banco de teste e devolve o cabeçalho
 * {@code Authorization} de cada usuário.
 */
@Component
public class Tokens {

    public static final String SENHA = "senha-de-teste-1";

    private final UsuarioRepository usuarios;
    private final CanteiroRepository canteiros;
    private final JwtService jwtService;
    private final PasswordEncoder encoderSenha;

    public Tokens(UsuarioRepository usuarios, CanteiroRepository canteiros, JwtService jwtService,
            PasswordEncoder encoderSenha) {
        this.usuarios = usuarios;
        this.canteiros = canteiros;
        this.jwtService = jwtService;
        this.encoderSenha = encoderSenha;
    }

    public Canteiro canteiro() {
        return canteiro("Canteiro " + UUID.randomUUID().toString().substring(0, 8));
    }

    public Canteiro canteiro(String nome) {
        return canteiros.saveAndFlush(new Canteiro(nome, null));
    }

    public Usuario almoxarife() {
        return almoxarife(canteiro().getId());
    }

    public Usuario criar(Perfil perfil, boolean administrador, UUID canteiroId) {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario usuario = new Usuario(perfil.name() + " " + sufixo, perfil.name().toLowerCase() + "." + sufixo
            + "@canteiro.test", encoderSenha.encode(SENHA), perfil, administrador, canteiroId);
        return usuarios.saveAndFlush(usuario);
    }

    public Usuario almoxarife(UUID canteiroId) {
        return criar(Perfil.ALMOXARIFE, false, canteiroId);
    }

    public Usuario gestor() {
        return criar(Perfil.GESTOR, false, null);
    }

    public Usuario admin() {
        return criar(Perfil.GESTOR, true, null);
    }

    public String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario).valor();
    }
}
