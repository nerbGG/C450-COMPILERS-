// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;
/**
 * An AST node for a break-statement.
 */
public class JBreakStatement extends JStatement {
    //declaring enclosing statement
    private JStatement enclosingStatement;
    /**
     * Constructs an AST node for a break-statement.
     *
     * @param line line in which the break-statement occurs in the source file.
     */
    public JBreakStatement(int line) {
        super(line);
    }

    /**
     * {@inheritDoc}
     */
    public JStatement analyze(Context context) {
        //getting a bunch of exceptions in each thing so adding if != null
        //setting it to the value at the top of the jmember enclosing statement stack
        enclosingStatement = JMember.enclosingStatement.peek();
        //set the enclosing statement's hasbreak to true
        if(enclosingStatement instanceof JForStatement){
            ((JForStatement) enclosingStatement).hasBreak = true;
        }
        else if(enclosingStatement instanceof JWhileStatement){
            ((JWhileStatement) enclosingStatement).hasBreak = true;
        }
        else if(enclosingStatement instanceof JDoStatement){
            ((JDoStatement) enclosingStatement).hasBreak = true;
        }
        else if(enclosingStatement instanceof JSwitchStatement){
            ((JSwitchStatement) enclosingStatement).hasBreak = true;
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        String breakLabel;//james helped with this
        //access the break label via the enclosing statement, and generate an unconditional jump to that label
        if(enclosingStatement instanceof JForStatement){
           // System.err.println(enclosingStatement.line());
            breakLabel = ((JForStatement) enclosingStatement).breakLabel;
            output.addBranchInstruction(GOTO, breakLabel);
        }
        else if(enclosingStatement instanceof JWhileStatement){
           // System.err.println(enclosingStatement.line());
            breakLabel = ((JWhileStatement) enclosingStatement).breakLabel;
            output.addBranchInstruction(GOTO, breakLabel);
        }
        else if(enclosingStatement instanceof JDoStatement){
            //System.err.println(enclosingStatement.line());
            breakLabel = ((JDoStatement) enclosingStatement).breakLabel;
            output.addBranchInstruction(GOTO, breakLabel);
        }
        else if(enclosingStatement instanceof JSwitchStatement){
            //System.err.println(enclosingStatement.line());
            breakLabel = ((JSwitchStatement) enclosingStatement).breakLabel;
            output.addBranchInstruction(GOTO, breakLabel);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JBreakStatement:" + line, e);
    }
}
