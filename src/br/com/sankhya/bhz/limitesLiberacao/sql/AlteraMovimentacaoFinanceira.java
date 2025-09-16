package br.com.sankhya.bhz.limitesLiberacao.sql;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.ws.BusinessException;

import java.util.Objects;

public class AlteraMovimentacaoFinanceira implements EventoProgramavelJava {
    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {

    }
    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        DynamicVO finVO = (DynamicVO) event.getVo();        // valores novos
        DynamicVO oldVO = (DynamicVO) event.getOldVO();     // valores antigos

        // Verifica se já estava conciliado antes
        Object dhConcilOld = oldVO.getProperty("DHCONCIL");
// evento para barrar estorno caso documento esteja conciliado
        // Se já estava conciliado
        if (dhConcilOld != null) {
            boolean alterouDhBaixa = !Objects.equals(finVO.getProperty("DHBAIXA"), oldVO.getProperty("DHBAIXA"));

            if (alterouDhBaixa) {
                throw new BusinessException(
                        "Não é permitido estornar títulos já conciliados. "
                                + "Gentileza consultar o setor Financeiro."
                );
            }
        }
    }

    @Override
    public void beforeDelete(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterInsert(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterUpdate(PersistenceEvent event) throws Exception {

    }

    @Override
    public void afterDelete(PersistenceEvent event) throws Exception {

    }

    @Override
    public void beforeCommit(TransactionContext tranCtx) throws Exception {

    }
}
