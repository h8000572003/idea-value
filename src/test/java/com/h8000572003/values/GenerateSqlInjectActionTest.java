package com.h8000572003.values;

public class GenerateSqlInjectActionTest {

    public static void main(String[] args) {

        String msg_no = null;
        String decl_type = null;
        String vessel_reg_no;
        vessel_reg_no = null;
        String car_no = null;
        String cont = null;
        String strSQL = " SELECT DISTINCT A.manif_no,A.goods_stat,A.amt,A.amt_unit,A.extr_cond," +
                " FROM SC_CTGDS_STATUSD A, VW_SC_5204_MASTER B" +
                " WHERE A.message_no='" + msg_no + "'" +
                " AND A.decl_type = " + " '%" + decl_type + "' and A.message_no ='2'";

    }
}