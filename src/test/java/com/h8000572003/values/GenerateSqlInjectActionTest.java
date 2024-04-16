package com.h8000572003.values;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class GenerateSqlInjectActionTest {

    public static void main(String[] args) {

        String msg_no = null;
        String decl_type = null;
        String vessel_reg_no;
        vessel_reg_no = null;
        String car_no = null;
        String cont = null;
        String strSQL = " SELECT DISTINCT A.manif_no,A.goods_stat,A.amt,A.amt_unit,A.extr_cond," +
                "A.goods_ban,nvl(A.goods_owner,' '),nvl(A.goods_name,' '),A.refer_no," +
                "A.shipping_co_code,A.grs_wgt,nvl(substr(B.type_of_clearance,1,2),'')," +  //20130923
                "nvl(B.package_released,''),nvl(B.package_unit,'')," +
                "nvl(B.extra_condition,''),nvl(B.exporter_ban_id,'')," +
                "nvl(B.exporter_english_name,''),nvl(B.c_c_c_code,'') " +
                " FROM SC_CTGDS_STATUSD A, VW_SC_5204_MASTER B" +
                " WHERE A.message_no = '" + msg_no + "'" +
                " AND A.decl_type = '" + decl_type + "'" +
                " AND A.vessel_reg_no = '" + vessel_reg_no + "'" +
                " AND A.car_no " + ((car_no.equals(" ") == true) ?
                " IS NULL " : "='" + car_no + "'") +
                " AND A.container_no " + ((cont.equals(" ") == true) ?
                " IS NULL " : "='" + cont + "'") +
                " AND B.vessel_reg_no(+) = '" + vessel_reg_no + "'" +
                " AND A.vessel_reg_no = B.vessel_reg_no(+) " +
                " AND A.manif_no = B.so_no(+) " +
                " AND B.package_released(+) > 0 " +
                " ORDER BY A.manif_no ";
    }
}