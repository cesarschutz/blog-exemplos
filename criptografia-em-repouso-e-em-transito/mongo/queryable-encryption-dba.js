// O DBA, conectado direto no banco e sem a chave: o trecho do artigo.
db = db.getSiblingDB("banco");
printjson(db.portadores.findOne());

// Sem a chave, a busca pelo valor em claro não encontra nada: o banco só guarda o CPF cifrado.
print("busca pelo CPF em claro, sem a chave:", db.portadores.countDocuments({ cpf: "123.456.789-09" }));
