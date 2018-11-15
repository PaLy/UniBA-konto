package sk.pluk64.unibakonto;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnibaKontoMock implements IsUnibaKonto {
    private List<Transaction> transactions;

    public UnibaKontoMock() {
        String transactions = "[\n" +
            "  {\n" +
            "    \"transactionItems\": [\n" +
            "      {\n" +
            "        \"timestamp\": \"14. 9. 2016 22:56:40\",\n" +
            "        \"service\": \"SYS\",\n" +
            "        \"shortcut\": \"ZAL\",\n" +
            "        \"description\": \"Záloha na ubytovanie\",\n" +
            "        \"amount\": \"-63,0000\",\n" +
            "        \"parsedAmount\": -63.0,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"\",\n" +
            "        \"payed\": \"13. 9. 2016 1:00:00\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"14. 9. 2016 22:56:49\",\n" +
            "        \"service\": \"DOB\",\n" +
            "        \"shortcut\": \"DOB\",\n" +
            "        \"description\": \"Platba z účtu\",\n" +
            "        \"amount\": \"100,0000\",\n" +
            "        \"parsedAmount\": 100.0,\n" +
            "        \"method\": \"B\",\n" +
            "        \"obj\": \"\",\n" +
            "        \"payed\": \"13. 9. 2016 1:00:00\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"transactionItems\": [\n" +
            "      {\n" +
            "        \"timestamp\": \"13. 9. 2016 18:16:33\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"ROS\",\n" +
            "        \"description\": \"Hov.rošt.prírodná 120 (1)\",\n" +
            "        \"amount\": \"-1,2000\",\n" +
            "        \"parsedAmount\": -1.2,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"13. 9. 2016 18:16:33\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"13. 9. 2016 18:16:33\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"RYZ\",\n" +
            "        \"description\": \"Ryža dusená 230g (9)\",\n" +
            "        \"amount\": \"-0,2500\",\n" +
            "        \"parsedAmount\": -0.25,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"13. 9. 2016 18:16:33\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"13. 9. 2016 18:16:33\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"TRO\",\n" +
            "        \"description\": \"Termoobal dvojdielny\",\n" +
            "        \"amount\": \"-0,1500\",\n" +
            "        \"parsedAmount\": -0.15,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"13. 9. 2016 18:16:33\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"transactionItems\": [\n" +
            "      {\n" +
            "        \"timestamp\": \"13. 9. 2016 17:58:03\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"KOL\",\n" +
            "        \"description\": \"Kur.s par.-oliv.om.120/150(1,3,5,6,7,8,9,10,11,13)\",\n" +
            "        \"amount\": \"-1,0000\",\n" +
            "        \"parsedAmount\": -1.0,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"13. 9. 2016 17:58:03\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"13. 9. 2016 17:58:04\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"RYZ\",\n" +
            "        \"description\": \"Ryža dusená 230g (9)\",\n" +
            "        \"amount\": \"-0,2500\",\n" +
            "        \"parsedAmount\": -0.25,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"13. 9. 2016 17:58:04\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"13. 9. 2016 17:58:04\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"FAZ\",\n" +
            "        \"description\": \"Fazuľová polievka s párkom (1,3,6,7,9,10,11)\",\n" +
            "        \"amount\": \"-0,4000\",\n" +
            "        \"parsedAmount\": -0.4,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"13. 9. 2016 17:58:04\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"13. 9. 2016 17:58:04\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"MRK\",\n" +
            "        \"description\": \"Mrkvový šalát 140g\",\n" +
            "        \"amount\": \"-0,4000\",\n" +
            "        \"parsedAmount\": -0.4,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"13. 9. 2016 17:58:04\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"transactionItems\": [\n" +
            "      {\n" +
            "        \"timestamp\": \"6. 9. 2016 10:57:36\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"RYZ\",\n" +
            "        \"description\": \"Ryža dusená 230g (9)\",\n" +
            "        \"amount\": \"-0,2500\",\n" +
            "        \"parsedAmount\": -0.25,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"6. 9. 2016 10:57:36\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"6. 9. 2016 10:57:36\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"RYZ\",\n" +
            "        \"description\": \"Ryža dusená 230g (9)\",\n" +
            "        \"amount\": \"-0,2500\",\n" +
            "        \"parsedAmount\": -0.25,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"6. 9. 2016 10:57:36\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"6. 9. 2016 10:57:36\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"KZS\",\n" +
            "        \"description\": \"Kur.zap.s faz.lúsk.,slan.a syrom (1,3,6,7,9,10,11)\",\n" +
            "        \"amount\": \"-1,0000\",\n" +
            "        \"parsedAmount\": -1.0,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"6. 9. 2016 10:57:36\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"6. 9. 2016 10:57:36\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"KPO\",\n" +
            "        \"description\": \"Kur.prsia s hubovou om.120g (1,7,9,10,11)\",\n" +
            "        \"amount\": \"-1,0000\",\n" +
            "        \"parsedAmount\": -1.0,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"6. 9. 2016 10:57:36\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"transactionItems\": [\n" +
            "      {\n" +
            "        \"timestamp\": \"6. 9. 2016 10:33:37\",\n" +
            "        \"service\": \"DOB\",\n" +
            "        \"shortcut\": \"DOB\",\n" +
            "        \"description\": \"Platba z účtu\",\n" +
            "        \"amount\": \"-25,0000\",\n" +
            "        \"parsedAmount\": -25.0,\n" +
            "        \"method\": \"B\",\n" +
            "        \"obj\": \"\",\n" +
            "        \"payed\": \"5. 9. 2016 1:00:00\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"transactionItems\": [\n" +
            "      {\n" +
            "        \"timestamp\": \"5. 9. 2016 10:42:43\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"BRA\",\n" +
            "        \"description\": \"Brav.zapekaný hamburský rezeň 120/150g (1,7,9,10)\",\n" +
            "        \"amount\": \"-1,0000\",\n" +
            "        \"parsedAmount\": -1.0,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"5. 9. 2016 10:42:43\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"5. 9. 2016 10:42:43\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"KQX\",\n" +
            "        \"description\": \"Kur.gratinov. so šampiň.,šun.120 (1,7,9,10,11)\",\n" +
            "        \"amount\": \"-1,0000\",\n" +
            "        \"parsedAmount\": -1.0,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"5. 9. 2016 10:42:43\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"5. 9. 2016 10:42:43\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"RYZ\",\n" +
            "        \"description\": \"Ryža dusená 230g (9)\",\n" +
            "        \"amount\": \"-0,2500\",\n" +
            "        \"parsedAmount\": -0.25,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"5. 9. 2016 10:42:43\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"5. 9. 2016 10:42:43\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"RYZ\",\n" +
            "        \"description\": \"Ryža dusená 230g (9)\",\n" +
            "        \"amount\": \"-0,2500\",\n" +
            "        \"parsedAmount\": -0.25,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"5. 9. 2016 10:42:43\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"transactionItems\": [\n" +
            "      {\n" +
            "        \"timestamp\": \"4. 9. 2016 17:16:35\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"RYZ\",\n" +
            "        \"description\": \"Ryža dusená 230g (9)\",\n" +
            "        \"amount\": \"-0,2500\",\n" +
            "        \"parsedAmount\": -0.25,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"4. 9. 2016 17:16:35\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"4. 9. 2016 17:16:35\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"FAZ\",\n" +
            "        \"description\": \"Fazuľová polievka kyslá (1,7)\",\n" +
            "        \"amount\": \"-0,4000\",\n" +
            "        \"parsedAmount\": -0.4,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"4. 9. 2016 17:16:35\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"4. 9. 2016 17:16:35\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"MRK\",\n" +
            "        \"description\": \"Mrkvový šalát 140g\",\n" +
            "        \"amount\": \"-0,4000\",\n" +
            "        \"parsedAmount\": -0.4,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"4. 9. 2016 17:16:35\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"4. 9. 2016 17:16:35\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"KPO\",\n" +
            "        \"description\": \"Kur.prsia s pažítk.om.120/150 (1,6,7,9,10,11)\",\n" +
            "        \"amount\": \"-1,0000\",\n" +
            "        \"parsedAmount\": -1.0,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"4. 9. 2016 17:16:35\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"transactionItems\": [\n" +
            "      {\n" +
            "        \"timestamp\": \"3. 9. 2016 11:59:31\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"HOV\",\n" +
            "        \"description\": \"Hov.mäso námornícke 120/150g (1,3)\",\n" +
            "        \"amount\": \"-1,1000\",\n" +
            "        \"parsedAmount\": -1.1,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"3. 9. 2016 11:59:31\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"3. 9. 2016 11:59:31\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"ZŠJ\",\n" +
            "        \"description\": \"Šalát zeleninový s jogurt.zálievkou220g(7)\",\n" +
            "        \"amount\": \"-0,8000\",\n" +
            "        \"parsedAmount\": -0.8,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"3. 9. 2016 11:59:31\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"3. 9. 2016 11:59:31\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"RYZ\",\n" +
            "        \"description\": \"Ryža dusená 230g (9)\",\n" +
            "        \"amount\": \"-0,2500\",\n" +
            "        \"parsedAmount\": -0.25,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"3. 9. 2016 11:59:31\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"3. 9. 2016 11:59:31\",\n" +
            "        \"service\": \"MEN\",\n" +
            "        \"shortcut\": \"HAS\",\n" +
            "        \"description\": \"Hašé polievka (1,3,6,7,9,10,11)\",\n" +
            "        \"amount\": \"-0,4000\",\n" +
            "        \"parsedAmount\": -0.4,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"ST\",\n" +
            "        \"payed\": \"3. 9. 2016 11:59:31\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"transactionItems\": [\n" +
            "      {\n" +
            "        \"timestamp\": \"1. 9. 2016 2:11:20\",\n" +
            "        \"service\": \"UBY\",\n" +
            "        \"shortcut\": \"UBY\",\n" +
            "        \"description\": \"Ubytovanie 31. 8. 2016 - 30. 9. 2016\",\n" +
            "        \"amount\": \"-63,0000\",\n" +
            "        \"parsedAmount\": -63.0,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"\",\n" +
            "        \"payed\": \"1. 9. 2016 2:11:21\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"timestamp\": \"1. 9. 2016 2:11:20\",\n" +
            "        \"service\": \"SYS\",\n" +
            "        \"shortcut\": \"ZAL\",\n" +
            "        \"description\": \"Použitie zálohy na ubytovanie\",\n" +
            "        \"amount\": \"63,0000\",\n" +
            "        \"parsedAmount\": 63.0,\n" +
            "        \"method\": \"K\",\n" +
            "        \"obj\": \"\",\n" +
            "        \"payed\": \"1. 9. 2016 2:11:20\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "]";

        this.transactions = new Gson().fromJson(
            transactions,
            new TypeToken<List<Transaction>>() {
            }.getType()
        );
    }

    @Override
    public String getUsername() {
        return "gump1";
    }

    @Override
    public String getPassword() {
        return "shrimp";
    }

    @Override
    public void login() {
    }

    @Override
    public Boolean isLoggedIn(boolean refresh) {
        return true;
    }

    @Override
    public Boolean isLoggedIn() {
        return true;
    }

    @Override
    public Map<String, Balance> getBalances() {
        Map<String, Balance> result = new HashMap<>();
        result.put(UnibaKonto.ID_ACCOUNT, new Balance("Konto", "42,50€"));
        result.put(UnibaKonto.ID_DEPOSIT, new Balance("Kaucia", "50,00€"));
        result.put(UnibaKonto.ID_DEPOSIT2, new Balance("Kaucia2", "0€"));
        result.put(UnibaKonto.ID_ZALOHA, new Balance("Záloha na ubyt.", "63€"));
        return result;
    }

    @Override
    public String getClientName() {
        return "Forrest Gump";
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return getTransactions();
    }

    @Override
    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    @Override
    public List<CardInfo> getCards() {
        return Collections.singletonList(
            new CardInfo(
                "32130757022430100",
                "1.9.2018",
                "1.9.2018",
                "30.9.2019"
            )
        );
    }
}
