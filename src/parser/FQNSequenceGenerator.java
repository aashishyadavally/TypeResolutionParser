package parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

public class FQNSequenceGenerator extends ASTVisitor {
    private static final String SEPARATOR = "#";
    
    private String className, superClassName;
    private int offset;
    private int numOfExpressions = 0, numOfResolvedExpressions = 0;
    private StringBuilder fullTokens = new StringBuilder(), partialTokens = new StringBuilder();
    private StringBuilder holeTokens = new StringBuilder(), fqnTokens = new StringBuilder();
    private String fullSequence = null, partialSequence = null;
    private String holeSequence = null, fqnSequence = null;
    private String[] fullSequenceTokens, partialSequenceTokens;
    private String[] holeSequenceTokens, fqnSequenceTokens;
    private ArrayList<HashMap<String, String>> nodeInfo = new ArrayList<HashMap<String, String>>();
    
    public FQNSequenceGenerator(String className, String superClassName, int offset) {
        super(false);
        this.className = className;
        this.superClassName = superClassName;
        this.offset = offset;
    }


    public ArrayList<HashMap<String, String>> getNodeInfo() {
        return nodeInfo;
    }

    public String[] getFullSequenceTokens() {
        if (fullSequenceTokens == null)
            buildFullSequence();
        return fullSequenceTokens;
    }

    public String[] getPartialSequenceTokens() {
        if (partialSequenceTokens == null)
            buildPartialSequence();
        return partialSequenceTokens;
    }

    public String[] getHoleSequenceTokens() {
        if (holeSequenceTokens == null)
            buildHoleSequence();
        return holeSequenceTokens;
    }

    public String[] getFQNSequenceTokens() {
        if (fqnSequenceTokens == null)
            buildFQNSequence();
        return fqnSequenceTokens;
    }

    public String getFullSequence() {
        if (fullSequence == null)
            buildFullSequence();
        return fullSequence;
    }

    public String getPartialSequence() {
        if (partialSequence == null)
            buildPartialSequence();
        return partialSequence;
    }

    public String getHoleSequence() {
        if (holeSequence == null)
            buildHoleSequence();
        return holeSequence;
    }

    public String getFQNSequence() {
        if (fqnSequence == null)
            buildFQNSequence();
        return fqnSequence;
    }

    private void buildFullSequence() {
        ArrayList<String> parts = buildSequence(fullTokens);
        this.fullSequence = parts.get(0);
        this.fullSequenceTokens = new String[parts.size() - 1];
        for (int i = 1; i < parts.size(); i++)
            this.fullSequenceTokens[i-1] = parts.get(i);
    }

    private void buildPartialSequence() {
        ArrayList<String> parts = buildSequence(partialTokens);
        this.partialSequence = parts.get(0);
        this.partialSequenceTokens = new String[parts.size() - 1];
        for (int i = 1; i < parts.size(); i++)
            this.partialSequenceTokens[i-1] = parts.get(i);
    }

    private void buildHoleSequence() {
        ArrayList<String> parts = buildSequence(holeTokens);
        this.holeSequence = parts.get(0);
        this.holeSequenceTokens = new String[parts.size() - 1];
        for (int i = 1; i < parts.size(); i++)
            this.holeSequenceTokens[i-1] = parts.get(i);
    }

    private void buildFQNSequence() {
        ArrayList<String> parts = buildSequence(fqnTokens);
        this.fqnSequence = parts.get(0);
        this.fqnSequenceTokens = new String[parts.size() - 1];
        for (int i = 1; i < parts.size(); i++)
            this.fqnSequenceTokens[i-1] = parts.get(i);
    }
    
    private ArrayList<String> buildSequence(StringBuilder tokens) {
        tokens.append(" ");
        ArrayList<String> l = new ArrayList<>();
        StringBuilder sequence = new StringBuilder(), token = null;
        for (int i = 0; i < tokens.length(); i++) {
            char ch = tokens.charAt(i);
            if (ch == ' ') {
                if (token != null) {
                    String t = token.toString();
                    l.add(t);
                    sequence.append(t + " ");
                    token = null;
                }
            } else {
                if (token == null)
                    token = new StringBuilder();
                token.append(ch);
            }
        }
        l.add(0, sequence.toString());
        return l;
    }

    public int getNumOfExpressions() {
        return numOfExpressions;
    }

    public int getNumOfResolvedExpressions() {
        return numOfResolvedExpressions;
    }

    private Type getType(VariableDeclarationFragment node) {
        ASTNode p = node.getParent();
        if (p instanceof VariableDeclarationExpression)
            return ((VariableDeclarationExpression) p).getType();
        if (p instanceof VariableDeclarationStatement)
            return ((VariableDeclarationStatement) p).getType();
        return null;
    }

    private String getSignature(IMethodBinding method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getTypeDeclaration().getQualifiedName());
        sb.append("." + method.getName());
        sb.append("(");
//        sb.append(SEPARATOR);
//        for (ITypeBinding tb : method.getParameterTypes())
//            sb.append(tb.getTypeDeclaration().getName() + "#");
        sb.append(")");
        return sb.toString();
    }

    static String getUnresolvedType(Type type) {
        if (type.isArrayType()) {
            ArrayType t = (ArrayType) type;
            return getUnresolvedType(t.getElementType()) + getDimensions(t.getDimensions());
        } else if (type.isIntersectionType()) {
            IntersectionType it = (IntersectionType) type;
            @SuppressWarnings("unchecked")
            ArrayList<Type> types = new ArrayList<>(it.types());
            String s = getUnresolvedType(types.get(0));
            for (int i = 1; i < types.size(); i++)
                s += " & " + getUnresolvedType(types.get(i));
            return s;
        } else if (type.isParameterizedType()) {
            ParameterizedType t = (ParameterizedType) type;
            return getUnresolvedType(t.getType());
        } else if (type.isUnionType()) {
            UnionType it = (UnionType) type;
            @SuppressWarnings("unchecked")
            ArrayList<Type> types = new ArrayList<>(it.types());
            String s = getUnresolvedType(types.get(0));
            for (int i = 1; i < types.size(); i++)
                s += " | " + getUnresolvedType(types.get(i));
            return s;
        } else if (type.isNameQualifiedType()) {
            NameQualifiedType qt = (NameQualifiedType) type;
            return qt.getQualifier().getFullyQualifiedName() + "." + qt.getName().getIdentifier();
        } else if (type.isPrimitiveType()) {
            return type.toString();
        } else if (type.isQualifiedType()) {
            QualifiedType qt = (QualifiedType) type;
            return getUnresolvedType(qt.getQualifier()) + "." + qt.getName().getIdentifier();
        } else if (type.isSimpleType()) {
            return type.toString();
        } else if (type.isWildcardType()) {
            WildcardType wt = (WildcardType) type;
            String s = "?";
            if (wt.getBound() != null) {
                if (wt.isUpperBound())
                    s += "extends ";
                else
                    s += "super ";
                s += getUnresolvedType(wt.getBound());
            }
            return s;
        }
        
        return null;
    }

    private static String getDimensions(int dimensions) {
        String s = "";
        for (int i = 0; i < dimensions; i++)
            s += "[]";
        return s;
    }

    static String getResolvedType(Type type) {
        ITypeBinding tb = type.resolveBinding();
        if (tb == null || tb.isRecovered())
            return getUnresolvedType(type);
        tb = tb.getTypeDeclaration();
        if (tb.isLocal() || tb.getQualifiedName().isEmpty())
            return getUnresolvedType(type);
        if (type.isArrayType()) {
            ArrayType t = (ArrayType) type;
            return getResolvedType(t.getElementType()) + getDimensions(t.getDimensions());
        } else if (type.isIntersectionType()) {
            IntersectionType it = (IntersectionType) type;
            @SuppressWarnings("unchecked")
            ArrayList<Type> types = new ArrayList<>(it.types());
            String s = getResolvedType(types.get(0));
            for (int i = 1; i < types.size(); i++)
                s += " & " + getResolvedType(types.get(i));
            return s;
        } else if (type.isParameterizedType()) {
            ParameterizedType t = (ParameterizedType) type;
            return getResolvedType(t.getType());
        } else if (type.isUnionType()) {
            UnionType it = (UnionType) type;
            @SuppressWarnings("unchecked")
            ArrayList<Type> types = new ArrayList<>(it.types());
            String s = getResolvedType(types.get(0));
            for (int i = 1; i < types.size(); i++)
                s += " | " + getResolvedType(types.get(i));
            return s;
        } else if (type.isNameQualifiedType()) {
            return tb.getQualifiedName();
        } else if (type.isPrimitiveType()) {
            return type.toString();
        } else if (type.isQualifiedType()) {
            return tb.getQualifiedName();
        } else if (type.isSimpleType()) {
            return tb.getQualifiedName();
        } else if (type.isWildcardType()) {
            WildcardType wt = (WildcardType) type;
            String s = "?";
            if (wt.getBound() != null) {
                if (wt.isUpperBound())
                    s += "extends ";
                else
                    s += "super ";
                s += getResolvedType(wt.getBound());
            }
            return s;
        }
        
        return null;
    }

    @Override
    public void preVisit(ASTNode node) {
        if (node instanceof Expression) {
            numOfExpressions++;
            Expression e = (Expression) node;
            if (e.resolveTypeBinding() != null && !e.resolveTypeBinding().isRecovered())
                numOfResolvedExpressions++;
        } else if (node instanceof Statement) {
            if (node instanceof ConstructorInvocation) {
                numOfExpressions++;
                if (((ConstructorInvocation) node).resolveConstructorBinding() != null && !((ConstructorInvocation) node).resolveConstructorBinding().isRecovered())
                    numOfResolvedExpressions++;
            } else if (node instanceof SuperConstructorInvocation) {
                numOfExpressions++;
                if (((SuperConstructorInvocation) node).resolveConstructorBinding() != null && !((SuperConstructorInvocation) node).resolveConstructorBinding().isRecovered())
                    numOfResolvedExpressions++;
            }
        } else if (node instanceof Type) {
            numOfExpressions++;
            Type t = (Type) node;
            if (t.resolveBinding() != null && !t.resolveBinding().isRecovered())
                numOfResolvedExpressions++;
        }
    }

    @Override
    public boolean visit(ArrayAccess node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ArrayCreation node) {
/*
        String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
        this.partialTokens.append(" new " + utype + " ");
        this.holeTokens.append(" new <blank>." + utype + " ");
        this.fullTokens.append(" new " + rtype + " ");
        this.fqnTokens.append("new " + rtype + " ");
        if (node.getInitializer() != null)
            node.getInitializer().accept(this);
        else
            for (int i = 0; i < node.dimensions().size(); i++)
                ((Expression) (node.dimensions().get(i))).accept(this);
        return false;
    }
*/
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ArrayInitializer node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(AssertStatement node) {
        this.fullTokens.append(" assert ");
        this.partialTokens.append(" assert ");

        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(Assignment node) {
        node.getLeftHandSide().accept(this);
        this.fullTokens.append(" = ");
        this.partialTokens.append(" = ");
        node.getRightHandSide().accept(this);

        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(Block node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(BooleanLiteral node) {
        this.fullTokens.append(" boolean ");
        this.partialTokens.append(" boolean ");

        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(BreakStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(CastExpression node) {
        String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
        this.fullTokens.append(" " + rtype + " <cast> ");
        this.partialTokens.append(" " + utype + " <cast> ");
        node.getExpression().accept(this);

        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("unresolvedType", utype);
            data.put("resolvedType", rtype);
            data.put("nodeType", "CAST_EXPRESSION");
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(CatchClause node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        this.fullTokens.append(" char ");
        this.partialTokens.append(" char ");

        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        ITypeBinding tb = node.getType().resolveBinding();
        if (tb != null && tb.getTypeDeclaration().isLocal())
            return false;
        String utype = getUnresolvedType(node.getType());
        String rtype = null;
        IMethodBinding b = node.resolveConstructorBinding();
        if (b == null) {
            this.fullTokens.append(" new " + utype);
            rtype = utype;
        } else {
            this.fullTokens.append(" new " + getSignature(b.getMethodDeclaration()) + " ");
            rtype = getSignature(b.getMethodDeclaration());
        }
        this.partialTokens.append(" new " + utype);
        for (Iterator it = node.arguments().iterator(); it.hasNext(); ) {
            Expression e = (Expression) it.next();
            e.accept(this);
        }
        if (node.getAnonymousClassDeclaration() != null)
            node.getAnonymousClassDeclaration().accept(this);

        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("unresolvedType", utype);
            data.put("resolvedType", rtype);
            data.put("nodeType", "CLASS_INSTANCE_CREATION");
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(ConditionalExpression node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        IMethodBinding b = node.resolveConstructorBinding();
        ITypeBinding tb = null;
        if (b != null && b.getDeclaringClass() != null)
            tb = b.getDeclaringClass().getTypeDeclaration();
        if (tb != null) {
            if (tb.isLocal() || tb.getQualifiedName().isEmpty())
                return false;
        }
        String name = "." + className;
        String utype = name;
        this.partialTokens.append(" " + name + " ");
        if (tb != null)
            name = getSignature(b.getMethodDeclaration());
        String rtype = name;
        this.fullTokens.append(" " + name + " ");
        for (int i = 0; i < node.arguments().size(); i++)
            ((ASTNode) node.arguments().get(i)).accept(this);

        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("unresolvedType", utype);
            data.put("resolvedType", rtype);
            data.put("nodeType", "CONSTRUCTOR_INVOCATION");
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(ContinueStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(CreationReference node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(Dimension node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(DoStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(EmptyStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(EnumConstantDeclaration node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(ExpressionMethodReference node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(FieldAccess node) {
        IVariableBinding b = node.resolveFieldBinding();
        ITypeBinding tb = null;
        if (b != null) {
            tb = b.getDeclaringClass();
            if (tb != null) {
                tb = tb.getTypeDeclaration();
                if (tb.isLocal() || tb.getQualifiedName().isEmpty())
                    return false;
            }
        }
        this.fullTokens.append(" ");
        this.partialTokens.append(" ");
        node.getExpression().accept(this);
        String name = "." + node.getName().getIdentifier();
        String utype = name;
        this.partialTokens.append(" " + name + " ");
        if (b != null) {
            if (tb != null)
                name = getQualifiedName(tb.getTypeDeclaration()) + name;
            /*else
                name = "Array" + name;*/
        }
        this.fullTokens.append(" " + name + " ");
        String rtype = name;

        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("unresolvedType", utype);
            data.put("resolvedType", rtype);
            data.put("nodeType", "FIELD_ACCESS");
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(ForStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(IfStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(ImportDeclaration node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(InfixExpression node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(Initializer node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(InstanceofExpression node) {
        this.fullTokens.append(" ");
        this.partialTokens.append(" ");
        node.getLeftOperand().accept(this);
        this.fullTokens.append(" <instanceof> ");
        this.partialTokens.append(" <instanceof> ");
        String rtype = getResolvedType(node.getRightOperand()), utype = getUnresolvedType(node.getRightOperand());
        this.fullTokens.append(rtype + " ");
        this.partialTokens.append(utype + " ");

        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("unresolvedType", utype);
            data.put("resolvedType", rtype);
            data.put("nodeType", "INSTANCEOF_EXPRESSION");
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(LabeledStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(LambdaExpression node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        if (node.getBody() != null && !node.getBody().statements().isEmpty())
            node.getBody().accept(this);

        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        int startPosition = node.getStartPosition();
        HashMap<String, String> data = new HashMap<String, String>();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
        }

        if (node.getExpression() != null && node.getExpression() instanceof TypeLiteral) {
            TypeLiteral lit = (TypeLiteral) node.getExpression();
            String utype = getUnresolvedType(lit.getType()), rtype = getResolvedType(lit.getType());
            this.fullTokens.append(" " + rtype + ".class." + node.getName().getIdentifier() + "() ");
            this.partialTokens.append(" " + utype + ".class." + node.getName().getIdentifier() + "() ");
            if (startPosition != -1) {
                data.put("unresolvedType", utype + node.getName().getIdentifier());
                data.put("resolvedType", rtype + node.getName().getIdentifier());
            }
        } else {
            IMethodBinding b = node.resolveMethodBinding();
            ITypeBinding tb = null;
            if (b != null) {
                tb = b.getDeclaringClass();
                if (tb != null) {
                    tb = tb.getTypeDeclaration();
                    if (tb.isLocal() || tb.getQualifiedName().isEmpty()) {
                        if (startPosition != -1) {
                            data.put("nodeType", "METHOD_INVOCATION");
                            nodeInfo.add(data);
                        }
                        return false;
                    }
                }
            }
            this.fullTokens.append(" ");
            this.partialTokens.append(" ");
            if (node.getExpression() != null) {
                node.getExpression().accept(this);
            } else {
                if (tb != null) {
                    this.partialTokens.append(" " + getName(tb) + " ");
                    this.fullTokens.append(" " + getQualifiedName(tb) + " ");
                    if (startPosition != -1) {
                        data.put("unresolvedType", getName(tb));
                        data.put("resolvedType", getQualifiedName(tb));
                    }
                } else {
                    this.partialTokens.append(" this ");
                    this.fullTokens.append(" this ");
                    if (startPosition != -1) {
                        data.put("unresolvedType", "this");
                        data.put("resolvedType", "this");
                    }
                }
            }
            String name = "."+ node.getName().getIdentifier() + "()";
            this.partialTokens.append(" " + name + " ");
            if (startPosition != -1) {
                data.put("unresolvedType", name);
            }
            if (tb != null)
                name = getSignature(b.getMethodDeclaration());
            this.fullTokens.append(" " + name + " ");
            if (startPosition != -1) {
                data.put("resolvedType", name);
            }
        }
        for (int i = 0; i < node.arguments().size(); i++)
            ((ASTNode) node.arguments().get(i)).accept(this);

        if (startPosition != -1) {
            data.put("nodeType", "METHOD_INVOCATION");
            nodeInfo.add(data);
        }
        return false;
    }

    @Override
    public boolean visit(Modifier node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(NormalAnnotation node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(NullLiteral node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        this.fullTokens.append(" null ");
        this.partialTokens.append(" null ");
        return false;
    }

    @Override
    public boolean visit(NumberLiteral node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        this.fullTokens.append(" number ");
        this.partialTokens.append(" number ");
        return false;
    }

    @Override
    public boolean visit(PackageDeclaration node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(ParenthesizedExpression node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(PostfixExpression node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(PrefixExpression node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(QualifiedName node) {
        int startPosition = node.getStartPosition();
        HashMap<String, String> data = new HashMap<String, String>();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        IBinding b = node.resolveBinding();
        IVariableBinding vb = null;
        ITypeBinding tb = null;
        if (b != null) {
            if (b instanceof IVariableBinding) {
                vb = (IVariableBinding) b;
                tb = vb.getDeclaringClass();
                if (tb != null) {
                    tb = tb.getTypeDeclaration();
                    if (tb.isLocal() || tb.getQualifiedName().isEmpty())
                        if (startPosition != -1) {
                            data.put("nodeType", "QUALIFIED_NAME");
                            nodeInfo.add(data);
                        }
                        return false;
                }
            } else if (b instanceof ITypeBinding) {
                tb = ((ITypeBinding) b).getTypeDeclaration();
                if (tb.isLocal() || tb.getQualifiedName().isEmpty())
                    return false;
                this.partialTokens.append(" " + node.getFullyQualifiedName() + " ");
                this.fullTokens.append(" " + getQualifiedName(tb) + " ");
                if (startPosition != -1) {
                    data.put("unresolvedType", node.getFullyQualifiedName());
                    data.put("resolvedType", getQualifiedName(tb));
                    data.put("nodeType", "QUALIFIED_NAME");
                    nodeInfo.add(data);
                }
                return false;
            }
        } else {
            this.partialTokens.append(" " + node.getFullyQualifiedName() + " ");
            this.fullTokens.append(" " + node.getFullyQualifiedName() + " ");
            if (startPosition != -1) {
                data.put("unresolvedType", node.getFullyQualifiedName());
                data.put("resolvedType", node.getFullyQualifiedName());
                data.put("nodeType", "QUALIFIED_NAME");
                nodeInfo.add(data);
            }
            return false;
        }
        node.getQualifier().accept(this);
        String name = "." + node.getName().getIdentifier();
        this.partialTokens.append(" " + name + " ");
        if (startPosition != -1)
            data.put("unresolvedType", name);
        if (b != null) {
            if (b instanceof IVariableBinding) {
                if (tb != null)
                    name = getQualifiedName(tb.getTypeDeclaration()) + name;
                /*else
                    name = "Array" + name;*/
            }
        }
        this.fullTokens.append(" " + name + " ");
        if (startPosition != -1) {
            data.put("resolvedType", name);
            data.put("nodeType", "QUALIFIED_NAME");
            nodeInfo.add(data);
        }
        return false;
    }

    @Override
    public boolean visit(ReturnStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(SimpleName node) {
        int startPosition = node.getStartPosition();
        HashMap<String, String> data = new HashMap<String, String>();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
        }

        IBinding b = node.resolveBinding();
        if (b != null) {
            if (b instanceof IVariableBinding) {
                IVariableBinding vb = (IVariableBinding) b;
                ITypeBinding tb = vb.getType();
                if (tb != null) {
                    tb = tb.getTypeDeclaration();
                    if (tb.isLocal() || tb.getQualifiedName().isEmpty()) {
                        if (startPosition != -1)
                            nodeInfo.add(data);
                        return false;
                    }
                    this.fullTokens.append(" " + getQualifiedName(tb) + " ");
                    this.partialTokens.append(" " + getName(tb) + " ");
                    if (startPosition != -1) {
                        data.put("unresolvedType", getQualifiedName(tb));
                        data.put("resolvedType", getName(tb));
                        data.put("nodeType", "SIMPLE_NAME");
                    }
                }
            } else if (b instanceof ITypeBinding) {
                ITypeBinding tb = (ITypeBinding) b;
                tb = tb.getTypeDeclaration();
                if (tb.isLocal() || tb.getQualifiedName().isEmpty()) {
                    if (startPosition != -1)
                        nodeInfo.add(data);
                    return false;
                }
                this.fullTokens.append(" " + getQualifiedName(tb) + " ");
                this.partialTokens.append(" " + getName(tb) + " ");
                if (startPosition != -1) {
                    data.put("unresolvedType", getQualifiedName(tb));
                    data.put("resolvedType", getName(tb));
                    data.put("nodeType", "SIMPLE_NAME");
                }
            }
        } else {
            this.fullTokens.append(" " + node.getIdentifier() + " ");
            this.partialTokens.append(" " + node.getIdentifier() + " ");
            if (startPosition != -1) {
                data.put("unresolvedType", node.getIdentifier());
                data.put("resolvedType", node.getIdentifier());
                data.put("nodeType", "SIMPLE_NAME");
            }
        }
        if (startPosition != -1)
            nodeInfo.add(data);
        return false;
    }

    @Override
    public boolean visit(SingleMemberAnnotation node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        HashMap<String, String> data = new HashMap<String, String>();
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
        }

        ITypeBinding tb = node.getType().resolveBinding();
        if (tb != null && tb.getTypeDeclaration().isLocal()) {
            if (startPosition != -1)
                nodeInfo.add(data);
            return false;
        }
        String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
        this.partialTokens.append(" " + utype + " ");
        this.fullTokens.append(" " + rtype + " ");
        if (startPosition != -1) {
            data.put("unresolvedType", utype);
            data.put("resolvedType", rtype);
            data.put("nodeType", "SINGLE_VARIABLE_DECLARATION");
        }
        if (node.getInitializer() != null) {
            this.partialTokens.append("= ");
            this.fullTokens.append("= ");
            node.getInitializer().accept(this);
        }
        if (startPosition != -1)
            nodeInfo.add(data);
        return false;
    }

    @Override
    public boolean visit(StringLiteral node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        this.fullTokens.append(" java.lang.String ");
        this.partialTokens.append(" java.lang.String ");
        return false;
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        HashMap<String, String> data = new HashMap<String, String>();
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
        }

        IMethodBinding b = node.resolveConstructorBinding();
        ITypeBinding tb = null;
        if (b != null && b.getDeclaringClass() != null)
            tb = b.getDeclaringClass().getTypeDeclaration();
        if (tb != null) {
            if (tb.isLocal() || tb.getQualifiedName().isEmpty()) {
                if (startPosition != -1)
                    nodeInfo.add(data);
                return false;
            }
        }
        String name = "." + superClassName;
        this.partialTokens.append(" " + name + " ");
        if (startPosition != -1)
            data.put("unresolvedType", name);
        if (tb != null)
            name = getSignature(b.getMethodDeclaration());
        this.fullTokens.append(" " + name + " ");
        if (startPosition != -1) {
            data.put("resolvedType", name);
            data.put("nodeType", "SUPER_FIELD_ACCESS");
            nodeInfo.add(data);
        }
        for (int i = 0; i < node.arguments().size(); i++)
            ((ASTNode) node.arguments().get(i)).accept(this);
        return false;
    }

    @Override
    public boolean visit(SuperFieldAccess node) {
        HashMap<String, String> data = new HashMap<String, String>();
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
        }

        IVariableBinding b = node.resolveFieldBinding();
        ITypeBinding tb = null;
        if (b != null && b.getDeclaringClass() != null) {
            tb = b.getDeclaringClass().getTypeDeclaration();
            if (tb.isLocal() || tb.getQualifiedName().isEmpty()) {
                if (startPosition != -1)
                    nodeInfo.add(data);
                return false;
            }
            this.partialTokens.append(" " + getName(tb) + " ");
            this.fullTokens.append(" " + getQualifiedName(tb) + " ");
            if (startPosition != -1) {
                data.put("unresolvedType", "" + getName(tb));
                data.put("resolvedType", "" + getQualifiedName(tb));
                data.put("nodeType", "SUPER_FIELD_ACCESS");
            }
        } else {
            this.partialTokens.append(" super ");
            this.fullTokens.append(" super ");
            if (startPosition != -1) {
                data.put("unresolvedType", "super");
                data.put("resolvedType", "super");
                data.put("nodeType", "SUPER_FIELD_ACCESS");
            }
        }
        String name = "." + node.getName().getIdentifier();
        this.partialTokens.append(" " + name + " ");
        if (startPosition != -1)
            data.put("unresolvedType", name);
        if (tb != null)
            name = getQualifiedName(tb) + name;
        this.fullTokens.append(" " + name + " ");
        if (startPosition != -1) {
            data.put("resolvedType", name);
            data.put("nodeType", "SUPER_FIELD_ACCESS");
            nodeInfo.add(data);
        }
        return false;
    }

    @Override
    public boolean visit(SuperMethodInvocation node) {
        HashMap<String, String> data = new HashMap<String, String>();
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
        }

        IMethodBinding b = node.resolveMethodBinding();
        ITypeBinding tb = null;
        if (b != null && b.getDeclaringClass() != null)
            tb = b.getDeclaringClass().getTypeDeclaration();
        if (tb != null) {
            if (tb.isLocal() || tb.getQualifiedName().isEmpty()) {
                if (startPosition != -1)
                    nodeInfo.add(data);
                return false;
            }
            this.partialTokens.append(" " + getName(tb) + " ");
            this.fullTokens.append(" " + getQualifiedName(tb) + " ");
            if (startPosition != -1) {
                data.put("unresolvedType", getName(tb));
                data.put("resolvedType", getQualifiedName(tb));
                data.put("nodeType", "SUPER_METHOD_INVOCATION");
            }
        } else {
            this.partialTokens.append(" super ");
            this.fullTokens.append(" super ");
            if (startPosition != -1) {
                data.put("unresolvedType", "super");
                data.put("resolvedType", "super");
                data.put("nodeType", "SUPER_METHOD_INVOCATION");
            }
        }
        String name = "." + node.getName().getIdentifier();
        this.partialTokens.append(" " + name + " ");
        if (startPosition != -1)
            data.put("unresolvedType", name);
        if (tb != null)
            name = getSignature(b.getMethodDeclaration());
        this.fullTokens.append(" " + name + " ");
        if (startPosition != -1) {
            data.put("unresolvedType", name);
            nodeInfo.add(data);
        }
        for (int i = 0; i < node.arguments().size(); i++)
            ((ASTNode) node.arguments().get(i)).accept(this);
        return false;
    }

    @Override
    public boolean visit(SuperMethodReference node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(SwitchCase node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(SwitchStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(SynchronizedStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(ThisExpression node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        ITypeBinding b = node.resolveTypeBinding();
        if (b != null) {
            b = b.getTypeDeclaration();
            if (b.isLocal() || b.getQualifiedName().isEmpty())
                return false;
            this.partialTokens.append(" " + getName(b) + " ");
            this.fullTokens.append(" " + getQualifiedName(b) + " ");
        } else {
            this.partialTokens.append(" this ");
            this.fullTokens.append(" this ");
        }
        return false;
    }

    @Override
    public boolean visit(ThrowStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(TryStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(TypeDeclarationStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    @Override
    public boolean visit(TypeLiteral node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
        this.fullTokens.append(" " + rtype + ".class ");
        this.partialTokens.append(" " + utype + ".class ");
        return false;
    }

    @Override
    public boolean visit(TypeMethodReference node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(TypeParameter node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }
    
    @Override
    public boolean visit(VariableDeclarationExpression node) {
        HashMap<String, String> data = new HashMap<String, String>();
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
        }

        ITypeBinding tb = node.getType().resolveBinding();
        if (tb != null && tb.getTypeDeclaration().isLocal()) {
            if (startPosition != -1) {
                nodeInfo.add(data);
            }
            return false;
        }
        String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
        this.partialTokens.append(" " + utype + " ");
        this.fullTokens.append(" " + rtype + " ");
        if (startPosition != -1) {
            data.put("unresolvedType", utype);
            data.put("resolvedType", rtype);
            data.put("nodeType", "VARIABLE_DECLARATION_EXPRESSION");
            nodeInfo.add(data);
        }
        for (int i = 0; i < node.fragments().size(); i++)
            ((ASTNode) node.fragments().get(i)).accept(this);
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        HashMap<String, String> data = new HashMap<String, String>();
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
        }

        ITypeBinding tb = node.getType().resolveBinding();
        if (tb != null && tb.getTypeDeclaration().isLocal()) {
            if (startPosition != -1) {
                nodeInfo.add(data);
            }
            return false;
        }
        String utype = getUnresolvedType(node.getType()), rtype = getResolvedType(node.getType());
        if (startPosition != -1) {
            data.put("unresolvedType", utype);
            data.put("resolvedType", rtype);
            data.put("nodeType", "VARIABLE_DECLARATION_STATEMENT");
            nodeInfo.add(data);
        }
        for (int i = 0; i < node.fragments().size(); i++)
            ((ASTNode) node.fragments().get(i)).accept(this);
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        HashMap<String, String> data = new HashMap<String, String>();
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
        }
        Type type = getType(node);
        String utype = getUnresolvedType(type), rtype = getResolvedType(type);
        this.partialTokens.append(" " + utype + " ");
        this.fullTokens.append(" " + rtype + " ");
        if (startPosition != -1) {
            data.put("unresolvedType", utype);
            data.put("resolvedType", rtype);
            data.put("nodeType", "VARIABLE_DECLARATION_FRAGMENT");
            nodeInfo.add(data);
        }
        if (node.getInitializer() != null) {
            this.partialTokens.append("= ");
            this.fullTokens.append("= ");
            node.getInitializer().accept(this);
        }
        return false;
    }

    @Override
    public boolean visit(WhileStatement node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return super.visit(node);
    }
    
    @Override
    public boolean visit(ArrayType node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }
    
    @Override
    public boolean visit(IntersectionType node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }
    
    @Override
    public boolean visit(ParameterizedType node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }
    
    @Override
    public boolean visit(UnionType node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }
    
    @Override
    public boolean visit(NameQualifiedType node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }
    
    @Override
    public boolean visit(PrimitiveType node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }
    
    @Override
    public boolean visit(QualifiedType node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }
    
    @Override
    public boolean visit(SimpleType node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }
    
    @Override
    public boolean visit(WildcardType node) {
        int startPosition = node.getStartPosition();
        if (startPosition != -1) {
            int endPosition = startPosition + node.getLength();
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("text", node.toString());
            data.put("start", "" + (startPosition - offset - 1));
            data.put("end", "" + (endPosition - offset - 1));
            nodeInfo.add(data);
        }

        return false;
    }

    private String getQualifiedName(ITypeBinding tb) {
        if (tb.isArray())
            return getQualifiedName(tb.getComponentType().getTypeDeclaration()) + getDimensions(tb.getDimensions());
        return tb.getQualifiedName();
    }

    private String getName(ITypeBinding tb) {
        if (tb.isArray())
            return getName(tb.getComponentType().getTypeDeclaration()) + getDimensions(tb.getDimensions());
        return tb.getName();
    }

}
