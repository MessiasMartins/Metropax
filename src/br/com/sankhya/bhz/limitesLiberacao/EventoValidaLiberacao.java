package br.com.sankhya.bhz.limitesLiberacao;

import br.com.sankhya.applauncher.ErrorUtils;
import br.com.sankhya.bhz.utils.ErroUtils;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import javax.xml.transform.Result;
import java.math.BigDecimal;
import java.sql.ResultSet;

public class EventoValidaLiberacao implements EventoProgramavelJava {
    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {

    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        DynamicVO libVO = (DynamicVO) event.getVo();
        if(event.getModifingFields().isModifingAny("DHLIB")
                && "N".equals(libVO.asString("REPROVADO"))
                && libVO.asBigDecimalOrZero("EVENTO").compareTo(BigDecimal.valueOf(1000))==0){
            JapeWrapper alcDAO = JapeFactory.dao("AD_GRUALC");
            JapeWrapper limDAO = JapeFactory.dao("AD_LIBLIM");
            JapeWrapper libDAO = JapeFactory.dao("LiberacaoLimite");

            BigDecimal nuChave = libVO.asBigDecimalOrZero("NUCHAVE");
            BigDecimal id = libVO.asBigDecimalOrZero("AD_ID");
            BigDecimal codusulib = libVO.asBigDecimalOrZero("CODUSULIB");

            /* Verifica se o Liberador tem 100% e exclui demais liberações do mesmo ID mesmo pedido.*/
            BigDecimal alcadaDia = BigDecimal.ZERO;
            BigDecimal alcadaMes = BigDecimal.ZERO;
            DynamicVO limVO = limDAO.findOne("ID = ?",id);
            if(null!= limVO && limVO.asBigDecimalOrZero("CODUSU").compareTo(codusulib)==0){
                alcadaDia = limVO.asBigDecimalOrZero("ALCADADIA");
                alcadaMes = limVO.asBigDecimalOrZero("ALCADAMES");
            }

            DynamicVO alcVO = alcDAO.findOne("CODUSU = ? AND ID = ?",codusulib,id);
            if(null!=alcVO){
                BigDecimal perc = alcVO.asBigDecimalOrZero("PERC");
                alcadaDia = alcVO.asBigDecimalOrZero("ALCADADIA");
                alcadaMes = alcVO.asBigDecimalOrZero("ALCADAMES");
                if(perc.compareTo(BigDecimal.valueOf(100))==0){
                        libDAO.deleteByCriteria("NUCHAVE = ? AND EVENTO = 1000 AND CODUSULIB != ? AND AD_ID = ?"
                                ,nuChave,codusulib,id);
                }
            }
            EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
            JdbcWrapper jdbc = dwfEntityFacade.getJdbcWrapper();
            NativeSql sql = new NativeSql(jdbc);
            sql.loadSql(this.getClass(), "sql/buscaAlcadasDisponivel.sql");
            sql.setNamedParameter("ID", id);
            sql.setNamedParameter("CODUSULIB", codusulib);
            ResultSet resultSet = sql.executeQuery();
            while (resultSet.next()){
                BigDecimal vlrLiberadoDia = resultSet.getBigDecimal("VLRLIBERADODIA");
                BigDecimal vlrLiberadoMes = resultSet.getBigDecimal("VLRLIBERADOMES");
                BigDecimal vlrLiberacao = libVO.asBigDecimalOrZero("VLRLIBERADO");
                BigDecimal novoLiberadoDia =vlrLiberadoDia.add(vlrLiberacao);
                BigDecimal novoLiberadoMes =vlrLiberadoMes.add(vlrLiberacao);

                if(novoLiberadoDia.compareTo(alcadaDia)>0){
                    ErroUtils.disparaErro("Liberação não permitida, Valor de liberação do dia ("
                            +novoLiberadoDia.toString()
                            +") ultrapassou a alçada cadastrada ("+alcadaDia.toString()+").");
                }

                if(novoLiberadoMes.compareTo(alcadaMes)>0){
                    ErroUtils.disparaErro("Liberação não permitida, Valor de liberação do Mês ("
                            +novoLiberadoMes.toString()
                            +") ultrapassou a alçada cadastrada ("+alcadaMes.toString()+").");
                }
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
