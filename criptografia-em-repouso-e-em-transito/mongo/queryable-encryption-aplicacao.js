// A aplicação, que tem a chave: cria a coleção com o CPF cifrado para consultas de igualdade, grava
// a portadora e consulta pelo CPF, como no artigo. A cifragem acontece no cliente (aqui, o mongosh,
// com a cifragem automática, que exige MongoDB Enterprise ou Atlas 7.0+ num replica set).
//
// A chave mestra é "local" (96 bytes gerados aqui) SÓ PARA ESTE TESTE: em produção ela fica num KMS
// que só a aplicação acessa (AWS KMS, Azure Key Vault, Google Cloud KMS ou KMIP).
const chaveMestra = require("crypto").randomBytes(96);
const opcoes = {
  keyVaultNamespace: "encryption.__keyVault",
  kmsProviders: { local: { key: chaveMestra } },
};
const conexao = Mongo("mongodb://localhost:27017/?replicaSet=rs0", opcoes);

conexao.getClientEncryption().createEncryptedCollection("banco", "portadores", {
  provider: "local",
  createCollectionOptions: {
    encryptedFields: {
      fields: [{ path: "cpf", bsonType: "string", queries: [{ queryType: "equality" }] }],
    },
  },
});

db = conexao.getDB("banco");
db.portadores.insertOne({ nome: "Maria Souza", cpf: "123.456.789-09" });

// O trecho do artigo:
printjson(db.portadores.find({ cpf: "123.456.789-09" }).toArray());
