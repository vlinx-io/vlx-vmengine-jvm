package io.vlinx.vmengine

// https://www.jianshu.com/p/d64a5dcccaa5

enum class Opcodes(val value: Short) {

    NOP(0),
    ACONST_NULL(1),

    // -1 入栈
    ICONST_M1(2),
    ICONST_0(3),
    ICONST_1(4),
    ICONST_2(5),
    ICONST_3(6),
    ICONST_4(7),
    ICONST_5(8),
    LCONST_0(9),
    LCONST_1(10),
    FCONST_0(11),
    FCONST_1(12),
    FCONST_2(13),
    DCONST_0(14),
    DCONST_1(15),

    // 当 int 取值 -128~127 时，JVM 采用 bipush 指令将常量压入栈中。
    BIPUSH(16),

    // 当 int 取值 -32768~32767 时，JVM 采用 sipush 指令将常量压入栈中。
    SIPUSH(17),

    // 将 int,float 或 String 型常量值从常量池中推送至栈顶
    LDC(18),

    // 将 int,float 或 String 型常量值从常量池中推送至栈顶(宽索引)
    LDC_W(19),

    // 将 long 或 double 型常量值从常量池中推送至栈顶(宽索引)
    LDC2_W(20),

    ILOAD(21),
    LLOAD(22),
    FLOAD(23),
    DLOAD(24),
    ALOAD(25),
    ILOAD_0(26),
    ILOAD_1(27),
    ILOAD_2(28),
    ILOAD_3(29),
    LLOAD_0(30),
    LLOAD_1(31),
    LLOAD_2(32),
    LLOAD_3(33),
    FLOAD_0(34),
    FLOAD_1(35),
    FLOAD_2(36),
    FLOAD_3(37),
    DLOAD_0(38),
    DLOAD_1(39),
    DLOAD_2(40),
    DLOAD_3(41),
    ALOAD_0(42),
    ALOAD_1(43),
    ALOAD_2(44),
    ALOAD_3(45),

    // load an int to stack from an array
    IALOAD(46),
    LALOAD(47),
    FALOAD(48),
    DALOAD(49),
    AALOAD(50),

    // byte[]
    BALOAD(51),

    // char[]
    CALOAD(52),

    // short[]
    SALOAD(53),

    ISTORE(54),
    LSTORE(55),
    FSTORE(56),
    DSTORE(57),
    ASTORE(58),

    ISTORE_0(59),
    ISTORE_1(60),
    ISTORE_2(61),
    ISTORE_3(62),
    LSTORE_0(63),
    LSTORE_1(64),
    LSTORE_2(65),
    LSTORE_3(66),
    FSTORE_0(67),
    FSTORE_1(68),
    FSTORE_2(69),
    FSTORE_3(70),
    DSTORE_0(71),
    DSTORE_1(72),
    DSTORE_2(73),
    DSTORE_3(74),
    ASTORE_0(75),
    ASTORE_1(76),
    ASTORE_2(77),
    ASTORE_3(78),
    IASTORE(79),
    LASTORE(80),
    FASTORE(81),
    DASTORE(82),
    AASTORE(83),
    BASTORE(84),
    CASTORE(85),
    SASTORE(86),
    POP(87),
    POP_2(88),

    // 复制栈顶数值并将复制值压入栈顶
    DUP(89),

    // 复制栈顶数值并将两个复制值压入栈顶
    DUP_X1(90),

    // 复制栈顶数值并将三个（或两个）复制值压入栈顶
    DUP_X2(91),

    DUP2(92),
    DUP2_X1(93),
    DUP2_X2(94),

    SWAP(95),

    IADD(96),
    LADD(97),
    FADD(98),
    DADD(99),
    ISUB(100),
    LSUB(101),
    FSUB(102),
    DSUB(103),
    IMUL(104),
    LMUL(105),
    FMUL(106),
    DMUL(107),
    IDIV(108),
    LDIV(109),
    FDIV(110),
    DDIV(111),

    // 余数
    IREM(112),
    LREM(113),
    FREM(114),
    DREM(115),

    INEG(116),
    LNEG(117),
    FNEG(118),
    DNEG(119),

    ISHL(120),
    LSHL(121),

    ISHR(122),
    LSHR(123),
    IUSHR(124),
    LUSHR(125),
    IAND(126),
    LAND(127),
    IOR(128),
    LOR(129),
    IXOR(130),
    LXOR(131),
    IINC(132),

    // 将栈顶int型数值强制转换成long型数值并将结果压入栈顶
    I2L(133),
    I2F(134),
    I2D(135),
    L2I(136),
    L2F(137),
    L2D(138),
    F2I(139),
    F2L(140),
    F2D(141),
    D2I(142),
    D2L(143),
    D2F(144),
    I2B(145),
    I2C(146),
    I2S(147),

    LCMP(148),

    // 对于double和float类型的数字，由于NaN的存在，各有两个版本的比较指令。
    // 以float为例，有fcmpg和fcmpl两个指令，它们的区别在于在数字比较时，若遇到NaN值，处理结果不同。
    FCMPL(149),
    FCMPG(150),
    DCMPL(151),
    DCMPG(152),

    IFEQ(153),
    IFNE(154),
    IFLT(155),
    IFGE(156),
    IFGT(157),
    IFLE(158),
    IF_ICMPEQ(159),
    IF_ICMPNE(160),
    IF_ICMPLT(161),
    IF_ICMPGE(162),
    IF_ICMPGT(163),
    IF_ICMPLE(164),
    IF_ACMPEQ(165),
    IF_ACMPNE(166),
    GOTO(167),

    // 跳转至指定16位offset位置，并将jsr下一条指令地址压入栈顶
    JSR(168),

    // 返回至本地变量指定的index的指令位置（一般与jsr, jsr_w联合使用）
    RET(169),

    // 用于switch条件跳转，case值连续（可变长度指令）
    TABLESWITCH(170),

    // 用于switch条件跳转，case值不连续（可变长度指令）
    LOOKUPSWITCH(171),

    IRETURN(172),
    LRETURN(172),
    FRETURN(173),
    DRETURN(174),
    ARETURN(175),
    RETURN(177),

    // 获取指定类的静态域，并将其值压入栈顶
    GETSTATIC(178),

    // 为指定的类的静态域赋值
    PUTSTATIC(179),

    GETFIELD(180),
    PUTFIELD(181),

    // 调用实例方法
    INVOKEVIRTUAL(182),

    // 调用超类构造方法，实例初始化方法，私有方法
    INVOKESPECIAL(183),
    INVOKESTATIC(184),

    // 调用接口方法
    INVOKEINTERFACE(185),

    // https://www.jianshu.com/p/d74e92f93752
    INVOKEDYNAMIC(186),

    NEW(187),

    // 原始类型数组
    NEWARRAY(188),

    // 引用类型数组
    ANEWARRAY(189),

    // 获得数组的长度值并压入栈顶
    ARRAYLENGTH(190),

    // 将栈顶的异常抛出
    ATHROW(191),

    // 检验类型转换，检验未通过将抛出ClassCastException
    CHECKCAST(192),
    INSTANCEOF(193),

    // 获得对象的锁，用于同步方法或同步块
    MONITORENTER(194),

    // 释放对象的锁，用于同步方法或同步块
    MONITOREXIT(195),

    // 当本地变量的索引超过255时使用该指令扩展索引宽度。
    WIDE(196),

    // TODO 多维数组？
    MULTIANEWARRAY(197),

    IFNULL(198),
    IFNONNULL(199),
    GOTO_W(200),
    JSR_W(201),
    BREAKPOINT(202),

    // TODO 需要理解 https://www.cnblogs.com/mongotea/p/11979755.html
    LDC_QUICK(203),
    LDC_W_QUICK(204),
    LDC2_W_QUICK(205),
    GETFIELD_QUICK(206),
    PUTFIELD_QUICK(207),
    GETFIELD2_QUICK(208),
    PUTFIELD2_QUICK(209),
    GETSTATIC_QUICK(210),
    PUTSTATIC_QUICK(211),
    GETSTATIC2_QUICK(212),
    PUTSTATIC2_QUICK(213),
    INVOKEVIRTUAL_QUICK(214),
    INVOKENONVIRTUAL_QUICK(215),
    INVOKESUPER_QUICK(216),
    INVOKESTATIC_QUICK(217),
    INVOKEINTERFACE_QUICK(218),
    INVOKEVIRTUALOBJECT_QUICK(219),
    NEW_QUICK(221),
    ANEWARRAY_QUICK(222),
    MULTIANEWARRAY_QUICK(223),
    CHECKCAST_QUICK(224),
    INSTANCEOF_QUICK(225),
    INVOKEVIRTUAL_QUICK_W(226),
    GETFIELD_QUICK_W(227),
    PUTFIELD_QUICK_W(228),

    // reserved for implementation-dependent operations within debuggers; should not appear in any class file
    IMPDEP1(254),
    IMPDEP2(255)


}

// Possible values for the type operand of the NEWARRAY instruction.
// See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html#jvms-6.5.newarray.
const val T_BOOLEAN = 4
const val T_CHAR = 5
const val T_FLOAT = 6
const val T_DOUBLE = 7
const val T_BYTE = 8
const val T_SHORT = 9
const val T_INT = 10
const val T_LONG = 11

fun getOpcode(op: Short): Opcodes {

    if (op in 229..253) {
        throw VlxVmException("Invalid opcode $op")
    }

    return Opcodes.values()[op.toInt()]

}