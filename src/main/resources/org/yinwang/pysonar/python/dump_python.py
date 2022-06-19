
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