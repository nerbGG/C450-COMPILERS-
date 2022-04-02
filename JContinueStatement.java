// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.GOTO;

/**
 * An AST node for a continue-statement.
 */
public class JContinueStatement extends JStatement {
    private JStatement enclosingStatement;
    /**
     * Constructs an AST node for a continue-statement.
     *
     * @param line line in which the continue-statement occurs in the source file.
     */
    public JContinueStatement(int line) {
        super(line);
    }

    /**
     * {@inheritDoc}
     */
    public JStatement analyze(Context context) {
        //copied from jbreak and removed switch
        enclosingStatement = JMember.enclosingStatement.peek();
        //set the enclosing statement's hasbreak to true
        if(enclosingStatement instanceof JForStatement){
            ((JForStatement) enclosingStatement).hasContinue = true;
        }
        else if(enclosingStatement instanceof JWhileStatement){
            ((JWhileStatement) enclosingStatement).hasContinue = true;
        }
        else if(enclosingStatement instanceof JDoStatement){
            ((JDoStatement) enclosingStatement).hasContinue = true;
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        String continueLabel;
        if(enclosingStatement instanceof JForStatement){
            // System.err.println(enclosingStatement.line());
            continueLabel = ((JForStatement) enclosingStatement).continueLabel;
            output.addBranchInstruction(GOTO, continueLabel);
        }
        else if(enclosingStatement instanceof JWhileStatement){
            // System.err.println(enclosingStatement.line());
            continueLabel = ((JWhileStatement) enclosingStatement).continueLabel;
            output.addBranchInstruction(GOTO, continueLabel);
        }
        else if(enclosingStatement instanceof JDoStatement){
            //System.err.println(enclosingStatement.line());
            continueLabel = ((JDoStatement) enclosingStatement).continueLabel;
            output.addBranchInstruction(GOTO, continueLabel);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JContinueStatement:" + line, e);
    }
}
