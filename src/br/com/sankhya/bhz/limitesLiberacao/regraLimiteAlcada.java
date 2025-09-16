package br.com.sankhya.bhz.limitesLiberacao;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.ContextoRegra;
import br.com.sankhya.modelcore.comercial.Regra;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;


public class regraLimiteAlcada implements Regra {

    @Override
    public void beforeInsert(ContextoRegra ctx) throws Exception {

    }

    @Override
    public void beforeUpdate(ContextoRegra ctx) throws Exception {
        JapeWrapper libDAO = JapeFactory.dao("LiberacaoLimite");
        boolean confirmando = JapeSession.getPropertyAsBoolean("CabecalhoNota.confirmando.nota", Boolean.FALSE);
        boolean tgfCab = "CabecalhoNota".equals(ctx.getPrePersistEntityState().getDao().getEntityName());
        if(confirmando && tgfCab) {
            DynamicVO cabVO = ctx.getPrePersistEntityState().getNewVO();
            if ("O".equals(cabVO.asString("TIPMOV"))) {

                BigDecimal nuNota = cabVO.asBigDecimalOrZero("NUNOTA");

                EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
                JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
                NativeSql sql = new NativeSql(jdbc);
                sql.loadSql(this.getClass(), "sql/buscaLiberadores.sql");
                sql.setNamedParameter("NUNOTA", nuNota);

                ResultSet result = sql.executeQuery();

                while (result.next()) {
                    BigDecimal codUsuLib = result.getBigDecimal("CODUSU");
                    String obs = result.getString("OBS");
                    String tabela = result.getString("TABELA");
                    BigDecimal sequencia = result.getBigDecimal("SEQUENCIA");
                    BigDecimal seqCascata = result.getBigDecimal("SEQCASCATA");
                    BigDecimal id = result.getBigDecimal("ID");

                    DynamicVO libVO = libDAO.findOne("NUCHAVE = ? AND TABELA = ? AND EVENTO = ? AND SEQUENCIA = ? AND SEQCASCATA = ?"
                            ,nuNota
                            ,tabela
                            ,BigDecimal.valueOf(1000)
                            ,sequencia
                            ,seqCascata);
                    if(null==libVO) {
                        libDAO.create()
                                .set("NUCHAVE", nuNota)
                                .set("TABELA", tabela)
                                .set("EVENTO", BigDecimal.valueOf(1000))
                                .set("CODUSUSOLICIT", AuthenticationInfo.getCurrent().getUserID())
                                .set("SEQUENCIA", sequencia)
                                .set("DHSOLICIT", TimeUtils.getNow())
                                .set("VLRATUAL", result.getBigDecimal("VLRTOT"))
                                .set("CODUSULIB", codUsuLib)
                                .set("OBSERVACAO", obs)
                                .set("VLRLIMITE", BigDecimal.ZERO)
                                .set("SEQCASCATA", seqCascata)
                                .set("AD_ID", id)
                                .save();
                    } else {
                        libDAO.prepareToUpdate(libVO)
                                .set("CODUSUSOLICIT", AuthenticationInfo.getCurrent().getUserID())
                                .set("VLRATUAL", result.getBigDecimal("VLRTOT"))
                                .update();
                    }
                }
            }
        }
    }

    @Override
    public void beforeDelete(ContextoRegra ctx) throws Exception {

    }

    @Override
    public void afterInsert(ContextoRegra ctx) throws Exception {

    }

    @Override
    public void afterUpdate(ContextoRegra ctx) throws Exception {

    }

    @Override
    public void afterDelete(ContextoRegra ctx) throws Exception {

    }
}
