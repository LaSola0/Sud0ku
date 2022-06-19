
import ast
import re
import sys
import codecs

from json import JSONEncoder
from ast import *


# Is it Python 3?
python3 = hasattr(sys.version_info, 'major') and (sys.version_info.major == 3)


class AstEncoder(JSONEncoder):
    def default(self, obj):
        if hasattr(obj, '__dict__'):
            dic = obj.__dict__
            # workaround: decode strings if it's not Python3 code
            if not python3:
                for key in dic:
                    if isinstance(dic[key], str):
                        if key == 's':
                            dic[key] = lines[dic['start']:dic['end']]
                        else:
                            dic[key] = dic[key].decode(enc)
            dic['pysonar_node_type'] = obj.__class__.__name__
            return dic
        else:
            return str(obj)


enc = 'latin1'
lines = ''


def parse_dump(filename, output, end_mark):
    try:
        if python3:
            encoder = AstEncoder()
        else:
            encoder = AstEncoder(encoding=enc)

        tree = parse_file(filename)
        encoded = encoder.encode(tree)
        f = open(output, "w")
        f.write(encoded)
        f.close()
    finally:
        # write marker file to signal write end
        f = open(end_mark, "w")
        f.close()


def parse_file(filename):
    global enc, lines
    enc, enc_len = detect_encoding(filename)
    f = codecs.open(filename, 'r', enc)
    lines = f.read()

    # remove BOM
    lines = re.sub(u'\ufeff', ' ', lines)

    # replace the encoding decl by spaces to fool python parser
    # otherwise you get 'encoding decl in unicode string' syntax error
    # print('enc:', enc, 'enc_len', enc_len)
    if enc_len > 0:
        lines = re.sub('#.*coding\s*[:=]\s*[\w\d\-]+', '#' + ' ' * (enc_len - 1), lines)

    f.close()
    return parse_string(lines, filename)


def parse_string(string, filename=None):
    tree = ast.parse(string)
    improve_ast(tree, string)
    if filename:
        tree.filename = filename
    return tree


# short function for experiments
def p(filename):
    parse_dump(filename, "json1", "end1")


def detect_encoding(path):
    fin = open(path, 'rb')
    prefix = str(fin.read(80))
    encs = re.findall('#.*coding\s*[:=]\s*([\w\d\-]+)', prefix)
    decl = re.findall('#.*coding\s*[:=]\s*[\w\d\-]+', prefix)

    if encs:
        enc1 = encs[0]
        enc_len = len(decl[0])
        try:
            codecs.lookup(enc1)
        except LookupError:
            return 'latin1', enc_len
        return enc1, enc_len
    else:
        return 'latin1', -1


#-------------------------------------------------------------
#                   improvements to the AST
#-------------------------------------------------------------
def improve_ast(node, s):
    build_index_map(s)
    improve_node(node, s)


line_starts = []


# build global table 'idxmap' for lineno <-> index conversion
def build_index_map(s):
    global line_starts
    idx = 0
    line_starts = [0]
    while idx < len(s):
        if s[idx] == '\n':
            line_starts.append(idx + 1)
        idx += 1


# convert (line, col) to offset index
def map_idx(line, col):
    return line_starts[line - 1] + col


# convert offset index into (line, col)
def map_line_col(idx):
    line = 0
    for start in line_starts:
        if idx < start:
            break
        line += 1
    col = idx - line_starts[line - 1]
    return line, col


def improve_node(node, s):
    if isinstance(node, list):
        for n in node:
            improve_node(n, s)

    elif isinstance(node, AST):
        find_start(node, s)
        find_end(node, s)
        if hasattr(node, 'start'):
            node.lineno, node.col_offset = map_line_col(node.start)
        add_missing_names(node, s)

        for f in node_fields(node):
            improve_node(f, s)


def find_start(node, s):
    ret = None    # default value

    if hasattr(node, 'start'):
        ret = node.start

    elif isinstance(node, list):
        if node != []:
            ret = find_start(node[0], s)

    elif isinstance(node, Module):
        if node.body != []:
            ret = find_start(node.body[0], s)

    elif isinstance(node, BinOp):
        leftstart = find_start(node.left, s)
        if leftstart is not None:
            ret = leftstart
        else:
            ret = map_idx(node.lineno, node.col_offset)

    elif hasattr(node, 'lineno'):
        if node.col_offset >= 0:
            ret = map_idx(node.lineno, node.col_offset)
        else:                           # special case for """ strings
            i = map_idx(node.lineno, node.col_offset)
            while i > 0 and i + 2 < len(s) and s[i:i + 3] != '"""' and s[i:i + 3] != "'''":
                i -= 1
            ret = i
    else:
        return None

    if ret is None and hasattr(node, 'lineno'):
        raise TypeError("got None for node that has lineno", node)

    if isinstance(node, AST) and ret is not None:
        node.start = ret

    return ret


def find_end(node, s):
    the_end = None

    if hasattr(node, 'end'):
        return node.end

    elif isinstance(node, list):
        if node != []:
            the_end = find_end(node[-1], s)

    elif isinstance(node, Module):
        if node.body != []:
            the_end = find_end(node.body[-1], s)

    elif isinstance(node, Expr):
        the_end = find_end(node.value, s)

    elif isinstance(node, Str):
        i = find_start(node, s)
        while s[i] != '"' and s[i] != "'":
            i += 1

        if i + 2 < len(s) and s[i:i + 3] == '"""':
            q = '"""'
            i += 3
        elif i + 2 < len(s) and s[i:i + 3] == "'''":
            q = "'''"
            i += 3
        elif s[i] == '"':
            q = '"'
            i += 1
        elif s[i] == "'":
            q = "'"
            i += 1
        else:
            print("illegal quote:", i, s[i])
            q = ''

        if q != '':
            the_end = end_seq(s, q, i)

    elif isinstance(node, Name):
        the_end = find_start(node, s) + len(node.id)

    elif isinstance(node, Attribute):
        the_end = end_seq(s, node.attr, find_end(node.value, s))

    elif isinstance(node, FunctionDef) or (python3 and isinstance(node, AsyncFunctionDef)):
        the_end = find_end(node.body, s)

    elif isinstance(node, Lambda):
        the_end = find_end(node.body, s)

    elif isinstance(node, ClassDef):
        the_end = find_end(node.body, s)

    # print will be a Call in Python 3
    elif not python3 and isinstance(node, Print):
        the_end = start_seq(s, '\n', find_start(node, s))

    elif isinstance(node, Call):
        start = find_end(node.func, s)
        if start is not None:
            the_end = match_paren(s, '(', ')', start)

    elif isinstance(node, Yield):
        the_end = find_end(node.value, s)

    elif isinstance(node, Return):
        if node.value is not None:
            the_end = find_end(node.value, s)
        else:
            the_end = find_start(node, s) + len('return')

    elif (isinstance(node, For) or
          isinstance(node, While) or
          isinstance(node, If) or
          isinstance(node, IfExp)):
        if node.orelse != []:
            the_end = find_end(node.orelse, s)
        else:
            the_end = find_end(node.body, s)

    elif isinstance(node, Assign) or isinstance(node, AugAssign):
        the_end = find_end(node.value, s)