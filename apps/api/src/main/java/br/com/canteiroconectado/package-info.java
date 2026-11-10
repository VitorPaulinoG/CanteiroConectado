/**
 * API do Canteiro Conectado.
 *
 * <p>O código é organizado em pacotes por funcionalidade ({@code autenticacao}, {@code usuario},
 * {@code canteiro}, {@code item}, {@code estoque}, {@code requisicao}, {@code auditoria}), cada um
 * com o próprio controller, service, repository, entidades e DTOs. A infraestrutura transversal
 * fica em {@code config}, {@code security}, {@code filter} e {@code exception}.</p>
 *
 * <p>Regra de dependência: controller chama só service; service chama repositórios e outros
 * services, nunca controllers; não há ciclo entre funcionalidades.</p>
 */
package br.com.canteiroconectado;
