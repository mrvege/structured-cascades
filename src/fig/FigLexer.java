package fig;

// $ANTLR 3.1.2 /home/djweiss/Downloads/java/Fig.g 2010-03-05 20:46:47

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class FigLexer extends Lexer {
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

    public FigLexer() {;} 
    public FigLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public FigLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "/home/djweiss/Downloads/java/Fig.g"; }

    // $ANTLR start "T__11"
    public final void mT__11() throws RecognitionException {
        try {
            int _type = T__11;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:3:7: ( '{' )
            // /home/djweiss/Downloads/java/Fig.g:3:9: '{'
            {
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__11"

    // $ANTLR start "T__12"
    public final void mT__12() throws RecognitionException {
        try {
            int _type = T__12;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:4:7: ( '}' )
            // /home/djweiss/Downloads/java/Fig.g:4:9: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__12"

    // $ANTLR start "T__13"
    public final void mT__13() throws RecognitionException {
        try {
            int _type = T__13;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:5:7: ( '=' )
            // /home/djweiss/Downloads/java/Fig.g:5:9: '='
            {
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__13"

    // $ANTLR start "T__14"
    public final void mT__14() throws RecognitionException {
        try {
            int _type = T__14;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:6:7: ( ';' )
            // /home/djweiss/Downloads/java/Fig.g:6:9: ';'
            {
            match(';'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__14"

    // $ANTLR start "T__15"
    public final void mT__15() throws RecognitionException {
        try {
            int _type = T__15;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:7:7: ( '$' )
            // /home/djweiss/Downloads/java/Fig.g:7:9: '$'
            {
            match('$'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__15"

    // $ANTLR start "T__16"
    public final void mT__16() throws RecognitionException {
        try {
            int _type = T__16;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:8:7: ( '[' )
            // /home/djweiss/Downloads/java/Fig.g:8:9: '['
            {
            match('['); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__16"

    // $ANTLR start "T__17"
    public final void mT__17() throws RecognitionException {
        try {
            int _type = T__17;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:9:7: ( ']' )
            // /home/djweiss/Downloads/java/Fig.g:9:9: ']'
            {
            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__17"

    // $ANTLR start "T__18"
    public final void mT__18() throws RecognitionException {
        try {
            int _type = T__18;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:10:7: ( ',' )
            // /home/djweiss/Downloads/java/Fig.g:10:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__18"

    // $ANTLR start "T__19"
    public final void mT__19() throws RecognitionException {
        try {
            int _type = T__19;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:11:7: ( '.' )
            // /home/djweiss/Downloads/java/Fig.g:11:9: '.'
            {
            match('.'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__19"

    // $ANTLR start "BL"
    public final void mBL() throws RecognitionException {
        try {
            int _type = BL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:55:4: ( 'true' | 'false' )
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0=='t') ) {
                alt1=1;
            }
            else if ( (LA1_0=='f') ) {
                alt1=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 1, 0, input);

                throw nvae;
            }
            switch (alt1) {
                case 1 :
                    // /home/djweiss/Downloads/java/Fig.g:55:6: 'true'
                    {
                    match("true"); 


                    }
                    break;
                case 2 :
                    // /home/djweiss/Downloads/java/Fig.g:55:13: 'false'
                    {
                    match("false"); 


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BL"

    // $ANTLR start "STRING"
    public final void mSTRING() throws RecognitionException {
        try {
            int _type = STRING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:56:8: ( '\"' ( . )* '\"' )
            // /home/djweiss/Downloads/java/Fig.g:56:10: '\"' ( . )* '\"'
            {
            match('\"'); 
            // /home/djweiss/Downloads/java/Fig.g:56:14: ( . )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0=='\"') ) {
                    alt2=2;
                }
                else if ( ((LA2_0>='\u0000' && LA2_0<='!')||(LA2_0>='#' && LA2_0<='\uFFFF')) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /home/djweiss/Downloads/java/Fig.g:56:14: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);

            match('\"'); 
            setText(getText().substring(1, 
            getText().length()-1));

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STRING"

    // $ANTLR start "INT"
    public final void mINT() throws RecognitionException {
        try {
            int _type = INT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:58:5: ( ( '-' )? ( '0' .. '9' )+ )
            // /home/djweiss/Downloads/java/Fig.g:58:9: ( '-' )? ( '0' .. '9' )+
            {
            // /home/djweiss/Downloads/java/Fig.g:58:9: ( '-' )?
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0=='-') ) {
                alt3=1;
            }
            switch (alt3) {
                case 1 :
                    // /home/djweiss/Downloads/java/Fig.g:58:10: '-'
                    {
                    match('-'); 

                    }
                    break;

            }

            // /home/djweiss/Downloads/java/Fig.g:58:15: ( '0' .. '9' )+
            int cnt4=0;
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( ((LA4_0>='0' && LA4_0<='9')) ) {
                    alt4=1;
                }


                switch (alt4) {
            	case 1 :
            	    // /home/djweiss/Downloads/java/Fig.g:58:15: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt4 >= 1 ) break loop4;
                        EarlyExitException eee =
                            new EarlyExitException(4, input);
                        throw eee;
                }
                cnt4++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "INT"

    // $ANTLR start "ID"
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:59:5: ( ( '_' | 'a' .. 'z' | 'A' .. 'Z' ) ( '_' | 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' )* )
            // /home/djweiss/Downloads/java/Fig.g:59:9: ( '_' | 'a' .. 'z' | 'A' .. 'Z' ) ( '_' | 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' )*
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // /home/djweiss/Downloads/java/Fig.g:59:33: ( '_' | 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' )*
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( ((LA5_0>='0' && LA5_0<='9')||(LA5_0>='A' && LA5_0<='Z')||LA5_0=='_'||(LA5_0>='a' && LA5_0<='z')) ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // /home/djweiss/Downloads/java/Fig.g:
            	    {
            	    if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop5;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ID"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:60:5: ( ( ' ' | '\\n' | '\\t' )+ )
            // /home/djweiss/Downloads/java/Fig.g:60:9: ( ' ' | '\\n' | '\\t' )+
            {
            // /home/djweiss/Downloads/java/Fig.g:60:9: ( ' ' | '\\n' | '\\t' )+
            int cnt6=0;
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( ((LA6_0>='\t' && LA6_0<='\n')||LA6_0==' ') ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // /home/djweiss/Downloads/java/Fig.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt6 >= 1 ) break loop6;
                        EarlyExitException eee =
                            new EarlyExitException(6, input);
                        throw eee;
                }
                cnt6++;
            } while (true);

            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WS"

    // $ANTLR start "CMT"
    public final void mCMT() throws RecognitionException {
        try {
            int _type = CMT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:61:5: ( '/*' ( . )* '*/' )
            // /home/djweiss/Downloads/java/Fig.g:61:9: '/*' ( . )* '*/'
            {
            match("/*"); 

            // /home/djweiss/Downloads/java/Fig.g:61:14: ( . )*
            loop7:
            do {
                int alt7=2;
                int LA7_0 = input.LA(1);

                if ( (LA7_0=='*') ) {
                    int LA7_1 = input.LA(2);

                    if ( (LA7_1=='/') ) {
                        alt7=2;
                    }
                    else if ( ((LA7_1>='\u0000' && LA7_1<='.')||(LA7_1>='0' && LA7_1<='\uFFFF')) ) {
                        alt7=1;
                    }


                }
                else if ( ((LA7_0>='\u0000' && LA7_0<=')')||(LA7_0>='+' && LA7_0<='\uFFFF')) ) {
                    alt7=1;
                }


                switch (alt7) {
            	case 1 :
            	    // /home/djweiss/Downloads/java/Fig.g:61:14: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop7;
                }
            } while (true);

            match("*/"); 

            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "CMT"

    // $ANTLR start "DOUBLE"
    public final void mDOUBLE() throws RecognitionException {
        try {
            int _type = DOUBLE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /home/djweiss/Downloads/java/Fig.g:62:8: ( ( '-' )? ( '0' .. '9' )+ '.' ( '0' .. '9' )+ )
            // /home/djweiss/Downloads/java/Fig.g:62:10: ( '-' )? ( '0' .. '9' )+ '.' ( '0' .. '9' )+
            {
            // /home/djweiss/Downloads/java/Fig.g:62:10: ( '-' )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0=='-') ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // /home/djweiss/Downloads/java/Fig.g:62:10: '-'
                    {
                    match('-'); 

                    }
                    break;

            }

            // /home/djweiss/Downloads/java/Fig.g:62:14: ( '0' .. '9' )+
            int cnt9=0;
            loop9:
            do {
                int alt9=2;
                int LA9_0 = input.LA(1);

                if ( ((LA9_0>='0' && LA9_0<='9')) ) {
                    alt9=1;
                }


                switch (alt9) {
            	case 1 :
            	    // /home/djweiss/Downloads/java/Fig.g:62:14: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt9 >= 1 ) break loop9;
                        EarlyExitException eee =
                            new EarlyExitException(9, input);
                        throw eee;
                }
                cnt9++;
            } while (true);

            match('.'); 
            // /home/djweiss/Downloads/java/Fig.g:62:26: ( '0' .. '9' )+
            int cnt10=0;
            loop10:
            do {
                int alt10=2;
                int LA10_0 = input.LA(1);

                if ( ((LA10_0>='0' && LA10_0<='9')) ) {
                    alt10=1;
                }


                switch (alt10) {
            	case 1 :
            	    // /home/djweiss/Downloads/java/Fig.g:62:26: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt10 >= 1 ) break loop10;
                        EarlyExitException eee =
                            new EarlyExitException(10, input);
                        throw eee;
                }
                cnt10++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "DOUBLE"

    public void mTokens() throws RecognitionException {
        // /home/djweiss/Downloads/java/Fig.g:1:8: ( T__11 | T__12 | T__13 | T__14 | T__15 | T__16 | T__17 | T__18 | T__19 | BL | STRING | INT | ID | WS | CMT | DOUBLE )
        int alt11=16;
        alt11 = dfa11.predict(input);
        switch (alt11) {
            case 1 :
                // /home/djweiss/Downloads/java/Fig.g:1:10: T__11
                {
                mT__11(); 

                }
                break;
            case 2 :
                // /home/djweiss/Downloads/java/Fig.g:1:16: T__12
                {
                mT__12(); 

                }
                break;
            case 3 :
                // /home/djweiss/Downloads/java/Fig.g:1:22: T__13
                {
                mT__13(); 

                }
                break;
            case 4 :
                // /home/djweiss/Downloads/java/Fig.g:1:28: T__14
                {
                mT__14(); 

                }
                break;
            case 5 :
                // /home/djweiss/Downloads/java/Fig.g:1:34: T__15
                {
                mT__15(); 

                }
                break;
            case 6 :
                // /home/djweiss/Downloads/java/Fig.g:1:40: T__16
                {
                mT__16(); 

                }
                break;
            case 7 :
                // /home/djweiss/Downloads/java/Fig.g:1:46: T__17
                {
                mT__17(); 

                }
                break;
            case 8 :
                // /home/djweiss/Downloads/java/Fig.g:1:52: T__18
                {
                mT__18(); 

                }
                break;
            case 9 :
                // /home/djweiss/Downloads/java/Fig.g:1:58: T__19
                {
                mT__19(); 

                }
                break;
            case 10 :
                // /home/djweiss/Downloads/java/Fig.g:1:64: BL
                {
                mBL(); 

                }
                break;
            case 11 :
                // /home/djweiss/Downloads/java/Fig.g:1:67: STRING
                {
                mSTRING(); 

                }
                break;
            case 12 :
                // /home/djweiss/Downloads/java/Fig.g:1:74: INT
                {
                mINT(); 

                }
                break;
            case 13 :
                // /home/djweiss/Downloads/java/Fig.g:1:78: ID
                {
                mID(); 

                }
                break;
            case 14 :
                // /home/djweiss/Downloads/java/Fig.g:1:81: WS
                {
                mWS(); 

                }
                break;
            case 15 :
                // /home/djweiss/Downloads/java/Fig.g:1:84: CMT
                {
                mCMT(); 

                }
                break;
            case 16 :
                // /home/djweiss/Downloads/java/Fig.g:1:88: DOUBLE
                {
                mDOUBLE(); 

                }
                break;

        }

    }


    protected DFA11 dfa11 = new DFA11(this);
    static final String DFA11_eotS =
        "\12\uffff\2\17\2\uffff\1\24\3\uffff\2\17\2\uffff\2\17\1\32\1\17"+
        "\1\uffff\1\32";
    static final String DFA11_eofS =
        "\34\uffff";
    static final String DFA11_minS =
        "\1\11\11\uffff\1\162\1\141\1\uffff\1\60\1\56\3\uffff\1\165\1\154"+
        "\2\uffff\1\145\1\163\1\60\1\145\1\uffff\1\60";
    static final String DFA11_maxS =
        "\1\175\11\uffff\1\162\1\141\1\uffff\2\71\3\uffff\1\165\1\154\2\uffff"+
        "\1\145\1\163\1\172\1\145\1\uffff\1\172";
    static final String DFA11_acceptS =
        "\1\uffff\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11\2\uffff\1\13\2\uffff"+
        "\1\15\1\16\1\17\2\uffff\1\14\1\20\4\uffff\1\12\1\uffff";
    static final String DFA11_specialS =
        "\34\uffff}>";
    static final String[] DFA11_transitionS = {
            "\2\20\25\uffff\1\20\1\uffff\1\14\1\uffff\1\5\7\uffff\1\10\1"+
            "\15\1\11\1\21\12\16\1\uffff\1\4\1\uffff\1\3\3\uffff\32\17\1"+
            "\6\1\uffff\1\7\1\uffff\1\17\1\uffff\5\17\1\13\15\17\1\12\6\17"+
            "\1\1\1\uffff\1\2",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\22",
            "\1\23",
            "",
            "\12\16",
            "\1\25\1\uffff\12\16",
            "",
            "",
            "",
            "\1\26",
            "\1\27",
            "",
            "",
            "\1\30",
            "\1\31",
            "\12\17\7\uffff\32\17\4\uffff\1\17\1\uffff\32\17",
            "\1\33",
            "",
            "\12\17\7\uffff\32\17\4\uffff\1\17\1\uffff\32\17"
    };

    static final short[] DFA11_eot = DFA.unpackEncodedString(DFA11_eotS);
    static final short[] DFA11_eof = DFA.unpackEncodedString(DFA11_eofS);
    static final char[] DFA11_min = DFA.unpackEncodedStringToUnsignedChars(DFA11_minS);
    static final char[] DFA11_max = DFA.unpackEncodedStringToUnsignedChars(DFA11_maxS);
    static final short[] DFA11_accept = DFA.unpackEncodedString(DFA11_acceptS);
    static final short[] DFA11_special = DFA.unpackEncodedString(DFA11_specialS);
    static final short[][] DFA11_transition;

    static {
        int numStates = DFA11_transitionS.length;
        DFA11_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA11_transition[i] = DFA.unpackEncodedString(DFA11_transitionS[i]);
        }
    }

    class DFA11 extends DFA {

        public DFA11(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 11;
            this.eot = DFA11_eot;
            this.eof = DFA11_eof;
            this.min = DFA11_min;
            this.max = DFA11_max;
            this.accept = DFA11_accept;
            this.special = DFA11_special;
            this.transition = DFA11_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__11 | T__12 | T__13 | T__14 | T__15 | T__16 | T__17 | T__18 | T__19 | BL | STRING | INT | ID | WS | CMT | DOUBLE );";
        }
    }
 

}