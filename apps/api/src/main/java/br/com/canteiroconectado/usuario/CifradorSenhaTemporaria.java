package br.com.canteiroconectado.usuario;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import br.com.canteiroconectado.config.SenhaTemporariaProperties;

/**
 * Cifra a senha temporária com AES-256-GCM para que só o administrador possa consultá-la.
 * O texto guardado é Base64 de {@code IV (12 bytes) || texto cifrado com tag}. O id do usuário
 * entra como dado autenticado, então a senha cifrada de um funcionário não serve para outro.
 */
@Component
public class CifradorSenhaTemporaria {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANHO_IV = 12;
    private static final int TAMANHO_TAG_BITS = 128;

    private final SecretKey chave;
    private final SecureRandom aleatorio = new SecureRandom();

    public CifradorSenhaTemporaria(SenhaTemporariaProperties propriedades) {
        this.chave = new SecretKeySpec(propriedades.chaveEmBytes(), "AES");
    }

    public String cifrar(String senha, UUID usuarioId) {
        byte[] iv = new byte[TAMANHO_IV];
        aleatorio.nextBytes(iv);
        try {
            Cipher cifra = Cipher.getInstance(ALGORITMO);
            cifra.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            cifra.updateAAD(usuarioId.toString().getBytes(StandardCharsets.UTF_8));
            byte[] cifrado = cifra.doFinal(senha.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length)
                .put(iv).put(cifrado).array());
        } catch (GeneralSecurityException falha) {
            throw new IllegalStateException("Não foi possível cifrar a senha temporária.", falha);
        }
    }

    /**
     * @throws IllegalStateException se o texto foi alterado, pertence a outro usuário ou foi
     *     cifrado com outra chave
     */
    public String decifrar(String cifrada, UUID usuarioId) {
        try {
            byte[] dados = Base64.getDecoder().decode(cifrada);
            Cipher cifra = Cipher.getInstance(ALGORITMO);
            cifra.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, dados, 0, TAMANHO_IV));
            cifra.updateAAD(usuarioId.toString().getBytes(StandardCharsets.UTF_8));
            byte[] aberto = cifra.doFinal(dados, TAMANHO_IV, dados.length - TAMANHO_IV);
            return new String(aberto, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException falha) {
            throw new IllegalStateException("Não foi possível decifrar a senha temporária.", falha);
        }
    }
}
