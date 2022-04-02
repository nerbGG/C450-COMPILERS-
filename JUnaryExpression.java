// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * This abstract base class is the AST node for an unary expression --- an expression with a
 * single operand.
 */
abstract class JUnaryExpression extends JExpression {
    /**
     * The unary operator.
     */
    protected String operator;

    /**
     * The operand.
     */
    protected JExpression operand;

    /**
     * Constructs an AST node for an unary expression.
     *
     * @param line     line in which the unary expression occurs in the source file.
     * @param operator the unary operator.
     * @param operand  the operand.
     */
    protected JUnaryExpression(int line, String operator, JExpression operand) {
        super(line);
        this.operator = operator;
        this.operand = operand;
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JUnaryExpression:" + line, e);
        e.addAttribute("operator", operator);
        e.addAttribute("type", type == null ? "" : type.toString());
        JSONElement e1 = new JSONElement();
        e.addChild("Operand", e1);
        operand.toJSON(e1);
    }
}

/**
 * The AST node for a logical NOT (!) expression.
 */
class JLogicalNotOp extends JUnaryExpression {
    /**
     * Constructs an AST for a logical NOT expression.
     *
     * @param line line in which the logical NOT expression occurs in the source file.
     * @param arg  the operand.
     */
    public JLogicalNotOp(int line, JExpression arg) {
        super(line, "!", arg);
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        operand = (JExpression) operand.analyze(context);
        operand.type().mustMatchExpected(line(), Type.BOOLEAN);
        type = Type.BOOLEAN;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        String falseLabel = output.createLabel();
        String trueLabel = output.createLabel();
        this.codegen(output, falseLabel, false);
        output.addNoArgInstruction(ICONST_1); // true
        output.addBranchInstruction(GOTO, trueLabel);
        output.addLabel(falseLabel);
        output.addNoArgInstruction(ICONST_0); // false
        output.addLabel(trueLabel);
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output, String targetLabel, boolean onTrue) {
        operand.codegen(output, targetLabel, !onTrue);
    }
}

/**
 * The AST node for a unary negation (-) expression.
 */
class JNegateOp extends JUnaryExpression {
    /**
     * Constructs an AST node for a negation expression.
     *
     * @param line    line in which the negation expression occurs in the source file.
     * @param operand the operand.
     */
    public JNegateOp(int line, JExpression operand) {
        super(line, "-", operand);
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        operand = operand.analyze(context);
        // operand.type().mustMatchExpected(line(), Type.INT);
        //type = Type.INT;
        operand.type().mustMatchOneOf(line(), Type.INT,Type.LONG, Type.DOUBLE);
        if(operand.type() == Type.INT) {
            type = Type.INT;
        }
        else if (operand.type() == Type.LONG) {
            type = Type.LONG;
        }
        else if (operand.type() == Type.DOUBLE) {
            type = Type.DOUBLE;
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        operand.codegen(output);
        if(type == Type.INT){
            output.addNoArgInstruction(INEG);
        }
        else  if(type == Type.LONG){
            output.addNoArgInstruction(LNEG);
        }
        else if(type == Type.DOUBLE){
            output.addNoArgInstruction(DNEG);
        }
    }
}

/**
 * The AST node for a post-decrement (--) expression.
 */
class JPostDecrementOp extends JUnaryExpression {
    /**
     * Constructs an AST node for a post-decrement expression.
     *
     * @param line    line in which the expression occurs in the source file.
     * @param operand the operand.
     */
    public JPostDecrementOp(int line, JExpression operand) {
        super(line, "-- (post)", operand);
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        if (!(operand instanceof JLhs)) {
            JAST.compilationUnit.reportSemanticError(line, "Operand to -- must have an LValue.");
            type = Type.ANY;
        } else {
            operand = (JExpression) operand.analyze(context);
            operand.type().mustMatchExpected(line(), Type.INT);
            type = Type.INT;
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        if (operand instanceof JVariable) {
            // A local variable; otherwise analyze() would have replaced it with an explicit
            // field selection.
            int offset = ((LocalVariableDefn) ((JVariable) operand).iDefn()).offset();
            if (!isStatementExpression) {
                // Loading its original rvalue.
                operand.codegen(output);
            }
            output.addIINCInstruction(offset, -1);
        } else {
            ((JLhs) operand).codegenLoadLhsLvalue(output);
            ((JLhs) operand).codegenLoadLhsRvalue(output);
            if (!isStatementExpression) {
                // Loading its original rvalue.
                ((JLhs) operand).codegenDuplicateRvalue(output);
            }
            output.addNoArgInstruction(ICONST_1);
            output.addNoArgInstruction(ISUB);
            ((JLhs) operand).codegenStore(output);
        }
    }
}

/**
 * The AST node for pre-increment (++) expression.
 */
class JPreIncrementOp extends JUnaryExpression {
    /**
     * Constructs an AST node for a pre-increment expression.
     *
     * @param line    line in which the expression occurs in the source file.
     * @param operand the operand.
     */
    public JPreIncrementOp(int line, JExpression operand) {
        super(line, "++ (pre)", operand);
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        if (!(operand instanceof JLhs)) {
            JAST.compilationUnit.reportSemanticError(line, "Operand to ++ must have an LValue.");
            type = Type.ANY;
        } else {
            operand = (JExpression) operand.analyze(context);
            operand.type().mustMatchExpected(line(), Type.INT);
            type = Type.INT;
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        if (operand instanceof JVariable) {
            // A local variable; otherwise analyze() would have replaced it with an explicit
            // field selection.
            int offset = ((LocalVariableDefn) ((JVariable) operand).iDefn()).offset();
            output.addIINCInstruction(offset, 1);
            if (!isStatementExpression) {
                // Loading its original rvalue.
                operand.codegen(output);
            }
        } else {
            ((JLhs) operand).codegenLoadLhsLvalue(output);
            ((JLhs) operand).codegenLoadLhsRvalue(output);
            output.addNoArgInstruction(ICONST_1);
            output.addNoArgInstruction(IADD);
            if (!isStatementExpression) {
                // Loading its original rvalue.
                ((JLhs) operand).codegenDuplicateRvalue(output);
            }
            ((JLhs) operand).codegenStore(output);
        }
    }
}

/**
 * The AST node for a unary plus (+) expression.
 */
class JUnaryPlusOp extends JUnaryExpression {
    /**
     * Constructs an AST node for a unary plus expression.
     *
     * @param line    line in which the unary plus expression occurs in the source file.
     * @param operand the operand.
     */
    public JUnaryPlusOp(int line, JExpression operand) {
        super(line, "+", operand);
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        operand = operand.analyze(context);
        //operand.type().mustMatchExpected(line(), Type.INT);
        //type = Type.INT;
        operand.type().mustMatchOneOf(line(), Type.INT,Type.LONG, Type.DOUBLE);
        // operand.type().mustMatchExpected(line(), Type.INT);
        if(operand.type() == Type.INT) {//maybe change to type = operand.type() instead of if else
            type = Type.INT;
        }
        else if (operand.type() == Type.LONG) {
            type = Type.LONG;
        }
        else if (operand.type() == Type.DOUBLE) {
            type = Type.DOUBLE;
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        operand.codegen(output);//not sure if there is an insruction for it
       //output.addNoArgInstruction(IADD);
//        if(type == Type.INT){
//            output.addNoArgInstruction(I);
//        }
//        else  if(type == Type.LONG){ // i forgot to put the intructions for long and double and james and I spent and hours looking at the decomplied bytecode for basictypes class to see that ity was doing ineg instead of dneg. directions only said to modify analyze for jnegetaop, didnt say anything about codegen
//            output.addNoArgInstruction(LNEG);
//        }
//        else if(type == Type.DOUBLE){
//            output.addNoArgInstruction(DNEG);
//        }
    }
}

/**
 * The AST node for a unary complement (~) expression.
 */
class JComplementOp extends JUnaryExpression {
    /**
     * Constructs an AST node for a unary complement expression.
     *
     * @param line    line in which the unary complement expression occurs in the source file.
     * @param operand the operand.
     */
    public JComplementOp(int line, JExpression operand) {
        super(line, "~", operand);
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        operand = operand.analyze(context);
        operand.type().mustMatchExpected(line(), Type.INT);
        type = Type.INT;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        operand.codegen(output);//does it load the input in the stack?
        output.addNoArgInstruction(ICONST_1);
        output.addNoArgInstruction(INEG);//negate 1
        output.addNoArgInstruction(IXOR);//exclusive or of operand and -1
    }
}

/**
 * The AST node for post-increment (++) expression.
 */
class JPostIncrementOp extends JUnaryExpression {
    /**
     * Constructs an AST node for a post-increment expression.
     *
     * @param line    line in which the expression occurs in the source file.
     * @param operand the operand.
     */
    public JPostIncrementOp(int line, JExpression operand) {
        super(line, "++ (post)", operand);
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
//        operand = operand.analyze(context);
//        operand.type().mustMatchExpected(line(), Type.INT);
//        type = Type.INT;
//        return this;
        if (!(operand instanceof JLhs)) {
            JAST.compilationUnit.reportSemanticError(line, "Operand to ++ must have an LValue.");
            type = Type.ANY;
        } else {
            operand = (JExpression) operand.analyze(context);
            operand.type().mustMatchExpected(line(), Type.INT);
            type = Type.INT;
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
//        operand.codegen(output);
//        output.addNoArgInstruction(IINC);
        if (operand instanceof JVariable) { //copied from postdecrement
            // A local variable; otherwise analyze() would have replaced it with an explicit
            // field selection.
            int offset = ((LocalVariableDefn) ((JVariable) operand).iDefn()).offset();
            if (!isStatementExpression) {
                // Loading its original rvalue.
                operand.codegen(output);
            }
            output.addIINCInstruction(offset, 1);
        } else {
            ((JLhs) operand).codegenLoadLhsLvalue(output);
            ((JLhs) operand).codegenLoadLhsRvalue(output);
            if (!isStatementExpression) {
                // Loading its original rvalue.
                ((JLhs) operand).codegenDuplicateRvalue(output);
            }
            output.addNoArgInstruction(ICONST_1);
            output.addNoArgInstruction(IADD); //add 1 to the value
            ((JLhs) operand).codegenStore(output);
        }
    }
}

/**
 * The AST node for a pre-decrement (--) expression.
 */
class JPreDecrementOp extends JUnaryExpression {
    /**
     * Constructs an AST node for a pre-decrement expression.
     *
     * @param line    line in which the expression occurs in the source file.
     * @param operand the operand.
     */
    public JPreDecrementOp(int line, JExpression operand) {
        super(line, "-- (pre)", operand);
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
//        operand = operand.analyze(context);
//        operand.type().mustMatchExpected(line(), Type.INT);
//        type = Type.INT;
//        return this;
        if (!(operand instanceof JLhs)) {
            JAST.compilationUnit.reportSemanticError(line, "Operand to -- must have an LValue.");
            type = Type.ANY;
        } else {
            operand = (JExpression) operand.analyze(context);
            operand.type().mustMatchExpected(line(), Type.INT);
            type = Type.INT;
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) { //subtract by 1
//        operand.codegen(output);
//        output.addNoArgInstruction(ICONST_1);
//        output.addNoArgInstruction(ISUB);
        if (operand instanceof JVariable) {
            // A local variable; otherwise analyze() would have replaced it with an explicit
            // field selection.
            int offset = ((LocalVariableDefn) ((JVariable) operand).iDefn()).offset();
            output.addIINCInstruction(offset, -1);
            if (!isStatementExpression) {
                // Loading its original rvalue.
                operand.codegen(output);
            }
        } else {
            ((JLhs) operand).codegenLoadLhsLvalue(output);
            ((JLhs) operand).codegenLoadLhsRvalue(output);
            output.addNoArgInstruction(ICONST_1);
            output.addNoArgInstruction(ISUB);
            if (!isStatementExpression) {
                // Loading its original rvalue.
                ((JLhs) operand).codegenDuplicateRvalue(output);
            }
            ((JLhs) operand).codegenStore(output);
        }
    }
}