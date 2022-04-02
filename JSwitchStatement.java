// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.beans.Expression;
import java.util.ArrayList;
import java.util.TreeMap;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a switch-statement.
 */
public class JSwitchStatement extends JStatement {
    // Test expression.
    private JExpression condition;

    // List of switch-statement groups.
    private ArrayList<SwitchStatementGroup> stmtGroup;
    public boolean hasBreak;
    public String breakLabel;
    public boolean hasContinue;
    public String continueLabel;
    public static ArrayList<Integer> caseNumbers ;

    public static int hi;
    public static int lo;
    public static int nLabels;
    public static boolean hasDefault;
    public static ArrayList<SwitchHolder> switches = new ArrayList<>();
    static int count;
    /**
     * Constructs an AST node for a switch-statement.
     *
     * @param line      line in which the switch-statement occurs in the source file.
     * @param condition test expression.
     * @param stmtGroup list of statement groups.
     */
    //project5/SwitchStatement.java
    public JSwitchStatement(int line, JExpression condition,
                            ArrayList<SwitchStatementGroup> stmtGroup) {
        super(line);
        this.condition = condition;
        this.stmtGroup = stmtGroup;
    }


    /**
     * {@inheritDoc}
     */
    public JStatement analyze(Context context) {
        hi = 0;
        lo = 999;
        nLabels = 0;
        caseNumbers = new ArrayList<>();
        JMember.enclosingStatement.push(this);
        //analyze the condition and make sure its an integer
        condition.analyze(context);
        condition.type().mustMatchExpected(line(),Type.INT);

        for(int i = 0; i <stmtGroup.size();i++ ){
            //analyze the statements in each casegroup in the new context
            //getting error bc of default, the contents of switchLabels are null
            stmtGroup.set(i, stmtGroup.get(i).analyze(context));
        }
        SwitchHolder holder = new SwitchHolder(hi, lo, nLabels, caseNumbers, stmtGroup, condition);
        switches.add(holder);
        JMember.enclosingStatement.pop();//james helped with this
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        //String endSwitch = output.createLabel();
        continueLabel  = output.createLabel();
        SwitchHolder sw = switches.get(count);
        //for(SwitchHolder sw: switches){ //created a loop to loop through switches but then realized that codegen is call after
            //if(count == switches.size()){
            //    break;
            //}
            breakLabel = output.createLabel();

            sw.condition.codegen(output);
            if(hasContinue){
                output.addLabel(continueLabel);
            }
            long tableSpaceCost = 5 + sw.hi - sw.lo;
            long tableTimeCost = 3;
            long lookupSpaceCost = 3 + 2 * sw.nLabels;
            long lookupTimeCost = sw.nLabels;
            int opcode = sw.nLabels > 0 && (tableSpaceCost + 3 * tableTimeCost <= lookupSpaceCost + 3 * lookupTimeCost) ? TABLESWITCH : LOOKUPSWITCH;
            if(opcode == TABLESWITCH){
                ArrayList<String> labels = new ArrayList<>();
                String defaultLabel = output.createLabel();
                //create a label for each case in each statement group
                //and add  it into a labels arrayList
                for(SwitchStatementGroup s : sw.stmtGroup){
                    ArrayList<JExpression> sL = s.getSwitchLabels();
                    for(JExpression caseL : sL){
                        //storing the labels to use in tableSwitchInstruction
                        labels.add(output.createLabel());
                    }
                }
                output.addTABLESWITCHInstruction(defaultLabel, sw.lo, sw.hi, labels);
                int i = 0;//accidently put this inside the forloop below so I was getting label 0 over and over again
                //generate code for the case group statements
                for(SwitchStatementGroup s : sw.stmtGroup){
                    output.addLabel(labels.get(i));//getting the label stored in the label arraylist
                    ArrayList<JStatement> block;
                    block = s.getBlock();//getting the case block to generate code later
                    for(JStatement b : block){
                        b.codegen(output);
                    }
                    i++;
                }
                output.addLabel(defaultLabel);
            }
            else if(opcode == LOOKUPSWITCH){
                String defaultLabel = output.createLabel();
                TreeMap<Integer, String> matchLabelPairs = new TreeMap<Integer, String>();
                for(int i=0; i <sw. nLabels; i++){
                    //ArrayList<JExpression> sL = stmtGroup.get(i).getSwitchLabels();
                    //for(int i =0; i<sL.size();i++){
                    //storing the labels to use in tableSwitchInstruction
                    matchLabelPairs.put(sw.caseNumbers.get(i), output.createLabel());
                    //}
                }
                output.addLOOKUPSWITCHInstruction(defaultLabel, matchLabelPairs.size(), matchLabelPairs);
                int i = 0;//accidently put this inside the forloop below so I was getting label 0 over and over again
                //generate code for the case group statements
                for(SwitchStatementGroup s : sw.stmtGroup){
                    if(i == matchLabelPairs.size()){
                        output.addLabel(defaultLabel);
                    }
                    else{
                        output.addLabel(matchLabelPairs.get(sw.caseNumbers.get(i)));//getting the label stored in the matchlabelpairs tree
                    }
                   ArrayList<JStatement> block;
                    block = s.getBlock();//getting the case block to generate code later
                    for(JStatement b :block){
                        b.codegen(output);
                    }
                    i++;
                }
            }
            if(hasBreak){
                output.addLabel(breakLabel);
            }
            count++;
        //}

        //output.addLabel(defaultLabel);

       // output.addLabel(endSwitch);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JSwitchStatement:" + line, e);
        JSONElement e1 = new JSONElement();
        e.addChild("Condition", e1);
        condition.toJSON(e1);
        for (SwitchStatementGroup group : stmtGroup) {
            group.toJSON(e);
        }
    }
}
//this will store the important switch variables
class SwitchHolder{
    public int hi;
    public  int lo;
    public int nLabels;
    public ArrayList<SwitchStatementGroup> stmtGroup;
    public ArrayList <Integer> caseNumbers;
    JExpression condition;
    SwitchHolder(int hi, int lo, int nLabels, ArrayList<Integer>  caseNumbers,
                 ArrayList<SwitchStatementGroup> stmtGroup, JExpression condition){
        this.hi = hi;
        this.lo = lo;
        this.caseNumbers = caseNumbers;
        this.nLabels = nLabels;
        this.stmtGroup = stmtGroup;
        this.condition = condition;
    }
}

/**
 * A switch statement group consists of case labels and a block of statements.
 */
class SwitchStatementGroup {

    // Case labels.
    private ArrayList<JExpression> switchLabels;

    // Block of statements.
    private ArrayList<JStatement> block;

    //case numbers (cases are only for ints)
   // public  ArrayList<Integer> caseNumbers = new ArrayList<>();

    public int high;
    public int low;

    public ArrayList<JExpression> getSwitchLabels() {
        return switchLabels;
    }
    public ArrayList<JStatement> getBlock() {
        return block;
    }
    /**
     * Constructs a switch-statement group.
     *
     * @param switchLabels case labels.
     * @param block        block of statements.
     */
    public SwitchStatementGroup(ArrayList<JExpression> switchLabels, ArrayList<JStatement> block) {
        this.switchLabels = switchLabels;
        this.block = block;
    }

    public SwitchStatementGroup analyze(Context context) {
        high  = 0;
        low = 999;
        for (int i =0; i < switchLabels.size();i++) {
            //switchLabel is a case, or a default
            if(switchLabels.get(i)!= null){
                JExpression switchLabel = switchLabels.get(i);
                switchLabels.set(i, switchLabel.analyze(context));
                if(switchLabel instanceof JLiteralInt){
                    int num = ((JLiteralInt) switchLabel).toInt();
                    high = num > high? num : high;//getting the highest case label value
                    low = num < low?  num: low;//getting the lowest case label value
                    JSwitchStatement.caseNumbers.add(num);
                    JSwitchStatement.nLabels++;
                }
                else{
                    JAST.compilationUnit.reportSemanticError(switchLabel.line(),
                            "expected case label to be an integer");
                }
                //sL.type().mustMatchExpected(sl.line(), Type.INT);
                //caseNumbers.add(i);
            }
            else{
                //was getting a nullpointer exception, after debugging,
                //found out that the default label is null
                JSwitchStatement.hasDefault = true;
            }
        }
        LocalContext lc = new LocalContext(context);
        for(int i =0; i<block.size();i++){//forgot to analyze block
            block.set(i, (JStatement) block.get(i).analyze(lc));
        }
        //compare all switchlabels to get high and low (doen above) and compare
        // each statementgroup to get the highest and lowest cases
        JSwitchStatement.hi = this.high > JSwitchStatement.hi ? this.high : JSwitchStatement.hi ;
        JSwitchStatement.lo = this.low < JSwitchStatement.lo  ? this.low : JSwitchStatement.lo;

        return this;
    }

    /**
     * Stores information about this switch statement group in JSON format.
     *
     * @param json the JSON emitter.
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("SwitchStatementGroup", e);
        for (JExpression label : switchLabels) {
            JSONElement e1 = new JSONElement();
            if (label != null) {
                e.addChild("Case", e1);
                label.toJSON(e1);
            } else {
                e.addChild("Default", e1);
            }
        }
        if (block != null) {
            for (JStatement stmt : block) {
                stmt.toJSON(e);
            }
        }
    }
}
