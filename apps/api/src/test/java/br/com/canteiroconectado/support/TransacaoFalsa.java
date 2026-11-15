package br.com.canteiroconectado.support;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Gerenciador de transação que não faz nada, para testes de unidade de services que usam
 * {@code TransactionTemplate}.
 */
public class TransacaoFalsa implements PlatformTransactionManager {

    private int confirmadas;
    private int desfeitas;

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definicao) {
        return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) {
        confirmadas++;
    }

    @Override
    public void rollback(TransactionStatus status) {
        desfeitas++;
    }

    public int confirmadas() {
        return confirmadas;
    }

    public int desfeitas() {
        return desfeitas;
    }
}
