package br.com.sankhya.bhz.acoesAgendadas;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.JdbcUtils;
import java.sql.ResultSet;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

public class AcaoAgendadaIntegraWLuto implements ScheduledAction {
    JapeWrapper conWLDAO = JapeFactory.dao("AD_CONWEBLUTO");

    public AcaoAgendadaIntegraWLuto() {
    }

    public void onTime(ScheduledActionContext arg0) {
        JdbcWrapper jdbc = null;
        NativeSql sql = null;
        ResultSet rset = null;
        JapeSession.SessionHandle hnd = null;

        try {
            hnd = JapeSession.open();
            EntityFacade entity = EntityFacadeFactory.getDWFFacade();
            jdbc = entity.getJdbcWrapper();
            jdbc.openSession();
            sql = new NativeSql(jdbc);
            sql.appendSql(" SELECT C.* ");
            sql.appendSql(" FROM OPENQUERY([MYSQL], 'SELECT * FROM metropax_banco.z_integracao_loja_planos_contract WHERE SINC_ECOMMERCE_ERP IS NULL OR SINC_ECOMMERCE_ERP = 0') C ");
            sql.appendSql(" LEFT JOIN AD_CONWEBLUTO L ON L.CODCONTRATO = C.ID ");
            sql.appendSql(" WHERE L.ID IS NULL;");
            rset = sql.executeQuery();

            while(rset.next()) {
                ((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)this.conWLDAO.create().set("CODCONTRATO", rset.getBigDecimal("id"))).set("PLANID", rset.getBigDecimal("plan_id"))).set("DATA", rset.getTimestamp("contract_date"))).set("SELLER_ID", rset.getBigDecimal("seller_id"))).set("PAYMENT_DAY", rset.getBigDecimal("payment_day"))).set("SIGNVINDI", rset.getString("signature_vindi_id"))).set("PARTNERID", rset.getBigDecimal("partner_id"))).save();
            }

            rset.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JdbcUtils.closeResultSet(rset);
            NativeSql.releaseResources(sql);
            JdbcWrapper.closeSession(jdbc);
            JapeSession.close(hnd);
        }

    }
}

