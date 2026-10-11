package com.transnacala.app.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.transnacala.app.model.Paragem;
import com.transnacala.app.model.Rota;

public class DatabaseSeeder {

    public static void inserirDadosIniciais(Context context) {

        RotaDAO rotaDAO = new RotaDAO(context);
        ParagemDAO paragemDAO = new ParagemDAO(context);
        RotaParagemDAO rotaParagemDAO = new RotaParagemDAO(context);

        /*
         * ============================================================
         * VERIFICAÇÃO DE SEGURANÇA (FIRST-RUN ONLY)
         * ============================================================
         * Se já existirem rotas no banco, encerra sem alterar nada.
         */

        if (rotaDAO.listarTodas().size() > 0) {
            return;
        }

        /*
         * ============================================================
         * 1. GARANTIR / ATUALIZAR AS ROTAS
         * ============================================================
         */

        inserirOuAtualizarRota(
                rotaDAO,
                new Rota(
                        "Baixa → Alta",
                        "Baixa",
                        "Alta",
                        15,
                        "Percurso entre a Baixa e a Alta."
                )
        );

        inserirOuAtualizarRota(
                rotaDAO,
                new Rota(
                        "Baixa → Matibane",
                        "Baixa",
                        "Matibane",
                        20,
                        "Percurso entre a Baixa e Matibane."
                )
        );

        inserirOuAtualizarRota(
                rotaDAO,
                new Rota(
                        "Fernão Veloso → Alta",
                        "Fernão Veloso",
                        "Alta",
                        15,
                        "Percurso entre Fernão Veloso e Alta."
                )
        );

        inserirOuAtualizarRota(
                rotaDAO,
                new Rota(
                        "Fernão Veloso → Juma",
                        "Fernão Veloso",
                        "Juma",
                        25,
                        "Percurso entre Fernão Veloso e Juma."
                )
        );

        inserirOuAtualizarRota(
                rotaDAO,
                new Rota(
                        "Juma → Nacala-a-Velha",
                        "Juma",
                        "Nacala-a-Velha",
                        50,
                        "Percurso entre Juma e Nacala-a-Velha."
                )
        );


        /*
         * ============================================================
         * 2. GARANTIR QUE AS PARAGENS EXISTEM
         * ============================================================
         */

        // ------------------------------------------------------------
        // Percurso Fernão Veloso
        // ------------------------------------------------------------

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Fernão Veloso",
                        -14.458744,
                        40.676286
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Naherenque",
                        -14.471189,
                        40.691133
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Entrada da base",
                        -14.490316,
                        40.695419
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Entrada do Aeroporto",
                        -14.494160,
                        40.695466
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "ASAMOC",
                        -14.503563,
                        40.697286
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "ADPP",
                        -14.505643,
                        40.697676
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Paira",
                        -14.521328,
                        40.695427
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Mocone",
                        -14.541633,
                        40.698412
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Sonho Real",
                        -14.551268,
                        40.697374
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Cruzamento Fernão Veloso",
                        -14.564426,
                        40.687693
                )
        );


        // ------------------------------------------------------------
        // Percurso Baixa
        // ------------------------------------------------------------

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Baixa",
                        -14.544387,
                        40.672718
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Rasta",
                        -14.543900,
                        40.676703
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Maiaia",
                        -14.543721,
                        40.678898
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Escadas",
                        -14.546111,
                        40.684148
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "VIP",
                        -14.548473,
                        40.684681
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Moti",
                        -14.554112,
                        40.685480
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Belmonte",
                        -14.558279,
                        40.686383
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Praça",
                        -14.556926,
                        40.690440
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Escola Secundária da Cidade Alta",
                        -14.555824,
                        40.691642
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Alta",
                        -14.555018,
                        40.692938
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Entrada da escola Santa Maria",
                        -14.561998,
                        40.687143
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Matibane",
                        -14.858443,
                        40.760088
                )
        );


        // ------------------------------------------------------------
        // Juma / Nacala-a-Velha
        // ------------------------------------------------------------

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Juma",
                        -14.571963,
                        40.683896
                )
        );

        inserirParagemSeNaoExistir(
                paragemDAO,
                new Paragem(
                        "Nacala-a-Velha",
                        -14.547094,
                        40.625267
                )
        );


        /*
         * ============================================================
         * 3. ASSOCIAR PARAGENS ÀS ROTAS
         * ============================================================
         */

        // ------------------------------------------------------------
        // ROTA 1
        // Fernão Veloso → Juma
        // ------------------------------------------------------------

        int rotaFernãoVelosoJuma =
                buscarIdRota(rotaDAO, "Fernão Veloso → Juma");

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoJuma,
                "Fernão Veloso",
                1
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoJuma,
                "Naherenque",
                2
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoJuma,
                "Entrada da base",
                3
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoJuma,
                "Entrada do Aeroporto",
                4
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoJuma,
                "ASAMOC",
                5
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoJuma,
                "ADPP",
                6
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoJuma,
                "Paira",
                7
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoJuma,
                "Mocone",
                8
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoJuma,
                "Sonho Real",
                9
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoJuma,
                "Cruzamento Fernão Veloso",
                10
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoJuma,
                "Juma",
                11
        );


        // ------------------------------------------------------------
        // ROTA 2
        // Fernão Veloso → Alta
        // ------------------------------------------------------------

        int rotaFernãoVelosoAlta =
                buscarIdRota(rotaDAO, "Fernão Veloso → Alta");

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoAlta,
                "Fernão Veloso",
                1
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoAlta,
                "Naherenque",
                2
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoAlta,
                "Entrada da base",
                3
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoAlta,
                "Entrada do Aeroporto",
                4
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoAlta,
                "ASAMOC",
                5
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoAlta,
                "ADPP",
                6
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoAlta,
                "Paira",
                7
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoAlta,
                "Mocone",
                8
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoAlta,
                "Sonho Real",
                9
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaFernãoVelosoAlta,
                "Alta",
                10
        );


        // ------------------------------------------------------------
        // ROTA 3
        // Baixa → Alta
        // ------------------------------------------------------------

        int rotaBaixaAlta =
                buscarIdRota(rotaDAO, "Baixa → Alta");

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaAlta,
                "Baixa",
                1
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaAlta,
                "Rasta",
                2
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaAlta,
                "Maiaia",
                3
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaAlta,
                "Escadas",
                4
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaAlta,
                "VIP",
                5
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaAlta,
                "Moti",
                6
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaAlta,
                "Belmonte",
                7
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaAlta,
                "Praça",
                8
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaAlta,
                "Escola Secundária da Cidade Alta",
                9
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaAlta,
                "Alta",
                10
        );


        // ------------------------------------------------------------
        // ROTA 4
        // Baixa → Matibane
        // ------------------------------------------------------------

        int rotaBaixaMatibane =
                buscarIdRota(rotaDAO, "Baixa → Matibane");

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaMatibane,
                "Baixa",
                1
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaMatibane,
                "Rasta",
                2
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaMatibane,
                "Maiaia",
                3
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaMatibane,
                "Escadas",
                4
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaMatibane,
                "VIP",
                5
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaMatibane,
                "Moti",
                6
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaMatibane,
                "Belmonte",
                7
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaMatibane,
                "Entrada da escola Santa Maria",
                8
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaMatibane,
                "Cruzamento Fernão Veloso",
                9
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaMatibane,
                "Juma",
                10
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaBaixaMatibane,
                "Matibane",
                11
        );


        // ------------------------------------------------------------
        // ROTA 5
        // Juma → Nacala-a-Velha
        // ------------------------------------------------------------

        int rotaJumaNacalaVelha =
                buscarIdRota(rotaDAO, "Juma → Nacala-a-Velha");

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaJumaNacalaVelha,
                "Juma",
                1
        );

        adicionarParagemNaRota(
                rotaParagemDAO,
                paragemDAO,
                rotaJumaNacalaVelha,
                "Nacala-a-Velha",
                2
        );
    }


    /*
     * ================================================================
     * MÉTODOS AUXILIARES
     * ================================================================
     */

    private static void inserirOuAtualizarRota(
            RotaDAO rotaDAO,
            Rota rota
    ) {

        int id = buscarIdRota(rotaDAO, rota.getNome());

        if (id == -1) {

            rotaDAO.inserir(rota);

        } else {

            atualizarRota(rotaDAO, id, rota);
        }
    }


    private static void atualizarRota(
            RotaDAO rotaDAO,
            int id,
            Rota rota
    ) {

        SQLiteDatabase db =
                rotaDAO.getDatabaseHelper()
                        .getWritableDatabase();

        android.content.ContentValues values =
                new android.content.ContentValues();

        values.put("nome", rota.getNome());
        values.put("origem", rota.getOrigem());
        values.put("destino", rota.getDestino());
        values.put("tarifa", rota.getTarifa());
        values.put("descricao", rota.getDescricao());

        db.update(
                "rota",
                values,
                "id = ?",
                new String[]{String.valueOf(id)}
        );

        db.close();
    }


    private static int buscarIdRota(
            RotaDAO rotaDAO,
            String nome
    ) {

        for (Rota rota : rotaDAO.listarTodas()) {

            if (rota.getNome().equals(nome)) {
                return rota.getId();
            }
        }

        return -1;
    }

    private static void inserirParagemSeNaoExistir(
            ParagemDAO paragemDAO,
            Paragem paragem
    ) {

        if (paragemDAO.buscarPorNome(paragem.getNome()) == null) {

            paragemDAO.inserir(paragem);
        }
    }

    private static void adicionarParagemNaRota(
            RotaParagemDAO rotaParagemDAO,
            ParagemDAO paragemDAO,
            int rotaId,
            String nomeParagem,
            int ordem
    ) {

        if (rotaId == -1) {
            return;
        }

        Paragem paragem =
                paragemDAO.buscarPorNome(nomeParagem);

        if (paragem == null) {
            return;
        }

        if (!relacaoExiste(
                rotaParagemDAO,
                rotaId,
                paragem.getId()
        )) {

            rotaParagemDAO.inserir(
                    rotaId,
                    paragem.getId(),
                    ordem
            );
        }
    }

    private static boolean relacaoExiste(
            RotaParagemDAO rotaParagemDAO,
            int rotaId,
            int paragemId
    ) {

        SQLiteDatabase db =
                rotaParagemDAO.getDatabaseHelper()
                        .getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id FROM rota_paragem " +
                        "WHERE rota_id = ? AND paragem_id = ? " +
                        "LIMIT 1",
                new String[]{
                        String.valueOf(rotaId),
                        String.valueOf(paragemId)
                }
        );

        boolean existe = cursor.moveToFirst();

        cursor.close();
        db.close();

        return existe;
    }
}