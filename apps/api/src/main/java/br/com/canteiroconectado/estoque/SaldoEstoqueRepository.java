package br.com.canteiroconectado.estoque;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface SaldoEstoqueRepository extends JpaRepository<SaldoEstoque, SaldoEstoqueId> {

    /**
     * Cria a linha de saldo zerada se ela ainda não existe. Duas primeiras movimentações
     * simultâneas do mesmo item não brigam: uma cria e a outra não faz nada.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
        INSERT INTO saldo_estoque (canteiro_id, item_id, quantidade, versao)
        VALUES (:canteiroId, :itemId, 0, 0)
        ON CONFLICT (canteiro_id, item_id) DO NOTHING
        """, nativeQuery = true)
    void criarSeAusente(UUID canteiroId, UUID itemId);

    /**
     * Lê o saldo com bloqueio de escrita ({@code SELECT ... FOR UPDATE}): outra transação que
     * queira o mesmo saldo espera esta terminar.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SaldoEstoque s where s.canteiroId = :canteiroId and s.itemId = :itemId")
    Optional<SaldoEstoque> travar(UUID canteiroId, UUID itemId);

    @Query("select s from SaldoEstoque s where s.canteiroId = :canteiroId and s.itemId = :itemId")
    Optional<SaldoEstoque> buscar(UUID canteiroId, UUID itemId);
}
