// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.util.ArrayList;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a for-statement.
 */
class JForStatement extends JStatement {
    // Initialization.
    private ArrayList<JStatement> init;

    // Test expression
    private JExpression condition;

    // Update.
    private ArrayList<JStatement> update;

    // The body.
    private JStatement body;

    private LocalContext context;
    public boolean hasBreak;
    public String breakLabel;
    public boolean hasContinue;
    public String continueLabel;
    /**
     * Constructs an AST node for a for-statement.
     *
     * @param line      line in which the for-statement occurs in the source file.
     * @param init      the initialization.
     * @param condition the test expression.
     * @param update    the update.
     * @param body      the body.
     */
    public JForStatement(int line, ArrayList<JStatement> init, JExpression condition,
                         ArrayList<JStatement> update, JStatement body) {
        super(line);
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;

    }

    /**
     * {@inheritDoc}
     */
    public JForStatement analyze(Context context1) {//looking at whileStatement for reference
        this.context = (LocalContext) context1; //james helped with this
        //push the current for loop onto enclosing statement
        JMember.enclosingStatement.push(this);//james helped with this
        if(init != null) {
            for(int i = 0; i < init.size(); i++){
                //System.out.println(init.get(i));
                init.set(i, (JStatement) init.get(i).analyze(context));
            }
        }
        if(condition != null){
            condition = condition.analyze(context);
            condition.type().mustMatchExpected(line(), Type.BOOLEAN);
        }
        if(update!=null) {
            for (int i = 0; i < update.size(); i++) {
                //   System.out.println(init.get(i));

                update.set(i, (JStatement) update.get(i).analyze(context));
            }
        }
        if(body!=null) {
            body = (JStatement) body.analyze(context);//analyzing body
        }
        JMember.enclosingStatement.pop();//james helped with this
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) { //using whilestatement codegen as reference
        String conditionLabel = output.createLabel();
        String endFor = output.createLabel();
        breakLabel = output.createLabel();
        continueLabel = output.createLabel(); //forgot to create the continue label so i got nullpointer exception
        if(init != null){
            for(JStatement i : init){
                i.codegen(output);
            }
        }
        output.addLabel(conditionLabel);
        output.addLabel(continueLabel);
        if(condition != null){
            condition.codegen(output, endFor, false);
        }

        //if(body != null){
            body.codegen(output);
       // }
//        if(hasBreak){
//            System.err.println("hasbreak is true");
//            output.addLabel(breakLabel);
//            output.addBranchInstruction(GOTO, out);
//        }
        output.addLabel(continueLabel);
        if(update != null){
            for(JStatement u : update){
                u.codegen(output);
            }
        }
        output.addBranchInstruction(GOTO, conditionLabel);
        if(hasBreak){
            output.addLabel(breakLabel);
        }

        output.addLabel(endFor);
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JForStatement:" + line, e);
        if (init != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Init", e1);
            for (JStatement stmt : init) {
                stmt.toJSON(e1);
            }
        }
        if (condition != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Condition", e1);
            condition.toJSON(e1);
        }
        if (update != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Update", e1);
            for (JStatement stmt : update) {
                stmt.toJSON(e1);
            }
        }
        if (body != null) {
            JSONElement e1 = new JSONElement();
            e.addChild("Body", e1);
            body.toJSON(e1);
        }
    }
}
