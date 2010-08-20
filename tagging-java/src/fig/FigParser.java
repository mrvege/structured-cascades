package fig;

// $ANTLR 3.1.2 /home/djweiss/Downloads/java/Fig.g 2010-03-05 20:46:46

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class FigParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "ID", "BL", "STRING", "INT", "DOUBLE", "WS", "CMT", "'{'", "'}'", "'='", "';'", "'$'", "'['", "']'", "','", "'.'"
    };
    public static final int INT=7;
    public static final int ID=4;
    public static final int EOF=-1;
    public static final int BL=5;
    public static final int T__19=19;
    public static final int CMT=10;
    public static final int T__16=16;
    public static final int WS=9;
    public static final int T__15=15;
    public static final int T__18=18;
    public static final int T__17=17;
    public static final int T__12=12;
    public static final int T__11=11;
    public static final int T__14=14;
    public static final int T__13=13;
    public static final int DOUBLE=8;
    public static final int STRING=6;

    // delegates
    // delegators


        public FigParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public FigParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return FigParser.tokenNames; }
    public String getGrammarFileName() { return "/home/djweiss/Downloads/java/Fig.g"; }


    Map instances = new HashMap();



    // $ANTLR start "file"
    // /home/djweiss/Downloads/java/Fig.g:13:1: file returns [List objects] : ( object )+ ;
    public final List file() throws RecognitionException {
        List objects = null;

        Object object1 = null;


        try {
            // /home/djweiss/Downloads/java/Fig.g:14:5: ( ( object )+ )
            // /home/djweiss/Downloads/java/Fig.g:14:9: ( object )+
            {
            objects = new ArrayList();
            // /home/djweiss/Downloads/java/Fig.g:15:9: ( object )+
            int cnt1=0;
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0==ID) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /home/djweiss/Downloads/java/Fig.g:15:10: object
            	    {
            	    pushFollow(FOLLOW_object_in_file43);
            	    object1=object();

            	    state._fsp--;

            	    objects.add(object1);

            	    }
            	    break;

            	default :
            	    if ( cnt1 >= 1 ) break loop1;
                        EarlyExitException eee =
                            new EarlyExitException(1, input);
                        throw eee;
                }
                cnt1++;
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return objects;
    }
    // $ANTLR end "file"


    // $ANTLR start "object"
    // /home/djweiss/Downloads/java/Fig.g:18:1: object returns [Object o] : qid (v= ID )? '{' ( assign[$o] )* '}' ;
    public final Object object() throws RecognitionException {
        Object o = null;

        Token v=null;
        FigParser.qid_return qid2 = null;


        try {
            // /home/djweiss/Downloads/java/Fig.g:19:5: ( qid (v= ID )? '{' ( assign[$o] )* '}' )
            // /home/djweiss/Downloads/java/Fig.g:19:9: qid (v= ID )? '{' ( assign[$o] )* '}'
            {
            pushFollow(FOLLOW_qid_in_object70);
            qid2=qid();

            state._fsp--;

            // /home/djweiss/Downloads/java/Fig.g:19:14: (v= ID )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==ID) ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // /home/djweiss/Downloads/java/Fig.g:19:14: v= ID
                    {
                    v=(Token)match(input,ID,FOLLOW_ID_in_object74); 

                    }
                    break;

            }


                    o = RunFig.newInstance((qid2!=null?input.toString(qid2.start,qid2.stop):null));
                    if ( v!=null ) {
                        instances.put((v!=null?v.getText():null), o);
                    }
                    
            match(input,11,FOLLOW_11_in_object95); 
            // /home/djweiss/Downloads/java/Fig.g:26:13: ( assign[$o] )*
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0==ID) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /home/djweiss/Downloads/java/Fig.g:26:13: assign[$o]
            	    {
            	    pushFollow(FOLLOW_assign_in_object97);
            	    assign(o);

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop3;
                }
            } while (true);

            match(input,12,FOLLOW_12_in_object101); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return o;
    }
    // $ANTLR end "object"


    // $ANTLR start "assign"
    // /home/djweiss/Downloads/java/Fig.g:29:1: assign[Object o] : ID '=' expr ';' ;
    public final void assign(Object o) throws RecognitionException {
        Token ID3=null;
        Object expr4 = null;


        try {
            // /home/djweiss/Downloads/java/Fig.g:30:5: ( ID '=' expr ';' )
            // /home/djweiss/Downloads/java/Fig.g:30:9: ID '=' expr ';'
            {
            ID3=(Token)match(input,ID,FOLLOW_ID_in_assign125); 
            match(input,13,FOLLOW_13_in_assign127); 
            pushFollow(FOLLOW_expr_in_assign129);
            expr4=expr();

            state._fsp--;

            match(input,14,FOLLOW_14_in_assign131); 
            RunFig.setObjectProperty(o,(ID3!=null?ID3.getText():null),expr4);

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "assign"


    // $ANTLR start "expr"
    // /home/djweiss/Downloads/java/Fig.g:34:1: expr returns [Object value] : ( qid (v= ID )? '{' ( assign[$value] )* '}' | BL | STRING | INT | DOUBLE | '$' ID | '[' ']' | '[' e= expr ( ',' e= expr )* ']' );
    public final Object expr() throws RecognitionException {
        Object value = null;

        Token v=null;
        Token BL6=null;
        Token STRING7=null;
        Token INT8=null;
        Token DOUBLE9=null;
        Token ID10=null;
        Object e = null;

        FigParser.qid_return qid5 = null;


        try {
            // /home/djweiss/Downloads/java/Fig.g:35:5: ( qid (v= ID )? '{' ( assign[$value] )* '}' | BL | STRING | INT | DOUBLE | '$' ID | '[' ']' | '[' e= expr ( ',' e= expr )* ']' )
            int alt7=8;
            alt7 = dfa7.predict(input);
            switch (alt7) {
                case 1 :
                    // /home/djweiss/Downloads/java/Fig.g:36:6: qid (v= ID )? '{' ( assign[$value] )* '}'
                    {
                    pushFollow(FOLLOW_qid_in_expr167);
                    qid5=qid();

                    state._fsp--;

                    // /home/djweiss/Downloads/java/Fig.g:36:11: (v= ID )?
                    int alt4=2;
                    int LA4_0 = input.LA(1);

                    if ( (LA4_0==ID) ) {
                        alt4=1;
                    }
                    switch (alt4) {
                        case 1 :
                            // /home/djweiss/Downloads/java/Fig.g:36:11: v= ID
                            {
                            v=(Token)match(input,ID,FOLLOW_ID_in_expr171); 

                            }
                            break;

                    }

                    value = RunFig.newInstance((qid5!=null?input.toString(qid5.start,qid5.stop):null));
                    match(input,11,FOLLOW_11_in_expr192); 
                    // /home/djweiss/Downloads/java/Fig.g:38:13: ( assign[$value] )*
                    loop5:
                    do {
                        int alt5=2;
                        int LA5_0 = input.LA(1);

                        if ( (LA5_0==ID) ) {
                            alt5=1;
                        }


                        switch (alt5) {
                    	case 1 :
                    	    // /home/djweiss/Downloads/java/Fig.g:38:13: assign[$value]
                    	    {
                    	    pushFollow(FOLLOW_assign_in_expr194);
                    	    assign(value);

                    	    state._fsp--;


                    	    }
                    	    break;

                    	default :
                    	    break loop5;
                        }
                    } while (true);

                    match(input,12,FOLLOW_12_in_expr198); 

                    }
                    break;
                case 2 :
                    // /home/djweiss/Downloads/java/Fig.g:39:9: BL
                    {
                    BL6=(Token)match(input,BL,FOLLOW_BL_in_expr216); 
                    value = Boolean.valueOf((BL6!=null?BL6.getText():null)); 

                    }
                    break;
                case 3 :
                    // /home/djweiss/Downloads/java/Fig.g:40:9: STRING
                    {
                    STRING7=(Token)match(input,STRING,FOLLOW_STRING_in_expr228); 
                    value = (STRING7!=null?STRING7.getText():null);

                    }
                    break;
                case 4 :
                    // /home/djweiss/Downloads/java/Fig.g:41:9: INT
                    {
                    INT8=(Token)match(input,INT,FOLLOW_INT_in_expr241); 
                    value = Integer.valueOf((INT8!=null?INT8.getText():null));

                    }
                    break;
                case 5 :
                    // /home/djweiss/Downloads/java/Fig.g:42:9: DOUBLE
                    {
                    DOUBLE9=(Token)match(input,DOUBLE,FOLLOW_DOUBLE_in_expr254); 
                    value = Double.valueOf((DOUBLE9!=null?DOUBLE9.getText():null)); 

                    }
                    break;
                case 6 :
                    // /home/djweiss/Downloads/java/Fig.g:43:9: '$' ID
                    {
                    match(input,15,FOLLOW_15_in_expr266); 
                    ID10=(Token)match(input,ID,FOLLOW_ID_in_expr268); 
                    value = instances.get((ID10!=null?ID10.getText():null));

                    }
                    break;
                case 7 :
                    // /home/djweiss/Downloads/java/Fig.g:44:9: '[' ']'
                    {
                    match(input,16,FOLLOW_16_in_expr281); 
                    match(input,17,FOLLOW_17_in_expr283); 
                    value = new ArrayList();

                    }
                    break;
                case 8 :
                    // /home/djweiss/Downloads/java/Fig.g:45:9: '[' e= expr ( ',' e= expr )* ']'
                    {
                    ArrayList elements = new ArrayList();
                    match(input,16,FOLLOW_16_in_expr305); 
                    pushFollow(FOLLOW_expr_in_expr309);
                    e=expr();

                    state._fsp--;

                    elements.add(e);
                    // /home/djweiss/Downloads/java/Fig.g:47:13: ( ',' e= expr )*
                    loop6:
                    do {
                        int alt6=2;
                        int LA6_0 = input.LA(1);

                        if ( (LA6_0==18) ) {
                            alt6=1;
                        }


                        switch (alt6) {
                    	case 1 :
                    	    // /home/djweiss/Downloads/java/Fig.g:47:14: ',' e= expr
                    	    {
                    	    match(input,18,FOLLOW_18_in_expr326); 
                    	    pushFollow(FOLLOW_expr_in_expr330);
                    	    e=expr();

                    	    state._fsp--;

                    	    elements.add(e);

                    	    }
                    	    break;

                    	default :
                    	    break loop6;
                        }
                    } while (true);

                    match(input,17,FOLLOW_17_in_expr344); 
                    value = elements;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return value;
    }
    // $ANTLR end "expr"

    public static class qid_return extends ParserRuleReturnScope {
    };

    // $ANTLR start "qid"
    // /home/djweiss/Downloads/java/Fig.g:52:1: qid : ID ( '.' ID | '$' ID )* ;
    public final FigParser.qid_return qid() throws RecognitionException {
        FigParser.qid_return retval = new FigParser.qid_return();
        retval.start = input.LT(1);

        try {
            // /home/djweiss/Downloads/java/Fig.g:52:5: ( ID ( '.' ID | '$' ID )* )
            // /home/djweiss/Downloads/java/Fig.g:52:9: ID ( '.' ID | '$' ID )*
            {
            match(input,ID,FOLLOW_ID_in_qid373); 
            // /home/djweiss/Downloads/java/Fig.g:52:12: ( '.' ID | '$' ID )*
            loop8:
            do {
                int alt8=3;
                int LA8_0 = input.LA(1);

                if ( (LA8_0==19) ) {
                    alt8=1;
                }
                else if ( (LA8_0==15) ) {
                    alt8=2;
                }


                switch (alt8) {
            	case 1 :
            	    // /home/djweiss/Downloads/java/Fig.g:52:13: '.' ID
            	    {
            	    match(input,19,FOLLOW_19_in_qid376); 
            	    match(input,ID,FOLLOW_ID_in_qid378); 

            	    }
            	    break;
            	case 2 :
            	    // /home/djweiss/Downloads/java/Fig.g:52:20: '$' ID
            	    {
            	    match(input,15,FOLLOW_15_in_qid380); 
            	    match(input,ID,FOLLOW_ID_in_qid382); 

            	    }
            	    break;

            	default :
            	    break loop8;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "qid"

    // Delegated rules


    protected DFA7 dfa7 = new DFA7(this);
    static final String DFA7_eotS =
        "\12\uffff";
    static final String DFA7_eofS =
        "\12\uffff";
    static final String DFA7_minS =
        "\1\4\6\uffff\1\4\2\uffff";
    static final String DFA7_maxS =
        "\1\20\6\uffff\1\21\2\uffff";
    static final String DFA7_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\1\6\1\uffff\1\7\1\10";
    static final String DFA7_specialS =
        "\12\uffff}>";
    static final String[] DFA7_transitionS = {
            "\1\1\1\2\1\3\1\4\1\5\6\uffff\1\6\1\7",
            "",
            "",
            "",
            "",
            "",
            "",
            "\5\11\6\uffff\2\11\1\10",
            "",
            ""
    };

    static final short[] DFA7_eot = DFA.unpackEncodedString(DFA7_eotS);
    static final short[] DFA7_eof = DFA.unpackEncodedString(DFA7_eofS);
    static final char[] DFA7_min = DFA.unpackEncodedStringToUnsignedChars(DFA7_minS);
    static final char[] DFA7_max = DFA.unpackEncodedStringToUnsignedChars(DFA7_maxS);
    static final short[] DFA7_accept = DFA.unpackEncodedString(DFA7_acceptS);
    static final short[] DFA7_special = DFA.unpackEncodedString(DFA7_specialS);
    static final short[][] DFA7_transition;

    static {
        int numStates = DFA7_transitionS.length;
        DFA7_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA7_transition[i] = DFA.unpackEncodedString(DFA7_transitionS[i]);
        }
    }

    class DFA7 extends DFA {

        public DFA7(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 7;
            this.eot = DFA7_eot;
            this.eof = DFA7_eof;
            this.min = DFA7_min;
            this.max = DFA7_max;
            this.accept = DFA7_accept;
            this.special = DFA7_special;
            this.transition = DFA7_transition;
        }
        public String getDescription() {
            return "34:1: expr returns [Object value] : ( qid (v= ID )? '{' ( assign[$value] )* '}' | BL | STRING | INT | DOUBLE | '$' ID | '[' ']' | '[' e= expr ( ',' e= expr )* ']' );";
        }
    }
 

    public static final BitSet FOLLOW_object_in_file43 = new BitSet(new long[]{0x0000000000000012L});
    public static final BitSet FOLLOW_qid_in_object70 = new BitSet(new long[]{0x0000000000000810L});
    public static final BitSet FOLLOW_ID_in_object74 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_11_in_object95 = new BitSet(new long[]{0x0000000000001010L});
    public static final BitSet FOLLOW_assign_in_object97 = new BitSet(new long[]{0x0000000000001010L});
    public static final BitSet FOLLOW_12_in_object101 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_assign125 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_13_in_assign127 = new BitSet(new long[]{0x00000000000189F0L});
    public static final BitSet FOLLOW_expr_in_assign129 = new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_14_in_assign131 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_qid_in_expr167 = new BitSet(new long[]{0x0000000000000810L});
    public static final BitSet FOLLOW_ID_in_expr171 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_11_in_expr192 = new BitSet(new long[]{0x0000000000001010L});
    public static final BitSet FOLLOW_assign_in_expr194 = new BitSet(new long[]{0x0000000000001010L});
    public static final BitSet FOLLOW_12_in_expr198 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BL_in_expr216 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_expr228 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INT_in_expr241 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOUBLE_in_expr254 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_15_in_expr266 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_ID_in_expr268 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_16_in_expr281 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_17_in_expr283 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_16_in_expr305 = new BitSet(new long[]{0x00000000000189F0L});
    public static final BitSet FOLLOW_expr_in_expr309 = new BitSet(new long[]{0x0000000000060000L});
    public static final BitSet FOLLOW_18_in_expr326 = new BitSet(new long[]{0x00000000000189F0L});
    public static final BitSet FOLLOW_expr_in_expr330 = new BitSet(new long[]{0x0000000000060000L});
    public static final BitSet FOLLOW_17_in_expr344 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_qid373 = new BitSet(new long[]{0x0000000000088002L});
    public static final BitSet FOLLOW_19_in_qid376 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_ID_in_qid378 = new BitSet(new long[]{0x0000000000088002L});
    public static final BitSet FOLLOW_15_in_qid380 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_ID_in_qid382 = new BitSet(new long[]{0x0000000000088002L});

}