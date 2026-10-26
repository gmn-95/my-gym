# Convenções de desenvolvimento do my-gym

Este arquivo contém decisões obrigatórias para qualquer alteração futura neste repositório. Leia também `SPEC.md` antes de modificar comportamento funcional.

## Escopo e princípios

- O aplicativo é pessoal, Android nativo, offline-first e sem backend.
- Implemente somente o escopo solicitado para a etapa atual.
- Dados locais próprios do usuário nunca podem depender da disponibilidade de uma API externa.
- Evite abstrações, módulos e dependências sem uma necessidade concreta.

## Identidade e compatibilidade

- O `applicationId` definitivo é `com.gmn95.mygym`; nunca o altere.
- Preserve a compatibilidade de atualização do APK.
- Aumente `versionCode` a cada APK release distribuído.
- Nunca versione keystore, senhas ou arquivos de propriedades de assinatura.

## Stack

- Kotlin, Jetpack Compose e Material 3.
- Gradle Kotlin DSL e catálogo de versões em `gradle/libs.versions.toml`.
- Room/SQLite para dados do domínio e preferências restauráveis.
- Coroutines e `Flow` para operações assíncronas e observação do banco.
- Um único módulo `app` enquanto não houver uma justificativa mensurável para modularização.

## Organização do código

- `ui`: composables, navegação, tema e estado visual.
- `feature`: ViewModels e estados de cada fluxo quando as funcionalidades forem criadas.
- `data/local`: entidades, DAOs, banco e migrações Room.
- `data/remote`: integrações externas isoladas.
- `data/repository`: acesso coordenado às fontes de dados.
- `domain`: regras puras quando uma regra não pertencer naturalmente a um ViewModel ou repositório.
- Composables não acessam DAO, Retrofit ou banco diretamente.

## Room e persistência

- IDs de entidades de domínio devem ser estáveis.
- Não use `fallbackToDestructiveMigration()`.
- Mantenha `exportSchema = true` e versione `app/schemas/`.
- Toda alteração de esquema exige aumento da versão, `Migration` explícita e teste com `MigrationTestHelper`.
- Operações compostas que não podem ficar parcialmente aplicadas devem usar transações.
- Exclusões de fichas não poderão apagar snapshots históricos.
- Estado necessário para recuperar uma sessão nunca deve existir apenas na memória da interface.

## Interface e recursos

- Todo texto próprio do app deve estar em recursos, com `values/` em inglês e `values-pt-rBR/` em português brasileiro.
- Não traduza nomes escritos pelo usuário.
- Preserve suporte a tema claro, escuro e preferência do sistema.
- Controles usados durante treinos devem ter alvos de toque adequados e rótulos acessíveis.
- Estados vazio, carregando e erro devem ser explícitos.

## Dependências e rede

- Adicione dependências apenas quando APIs Android/Compose existentes não forem suficientes.
- Centralize versões no catálogo e use apenas versões estáveis.
- Integrações externas devem ficar atrás de uma interface de repositório.
- A futura ExerciseDB não poderá bloquear fichas, sessões ou histórico offline.
- Não persista nem exporte conteúdo remoto sem confirmar que os termos do provedor permitem isso.

## Testes e validação

- Regras de negócio devem ser testadas sem depender da interface.
- DAOs, transações e migrações devem ter testes instrumentados.
- ViewModels devem usar dependências substituíveis para permitir testes determinísticos.
- Antes de concluir uma alteração, execute os comandos aplicáveis:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

- Nunca declare que uma compilação ou teste passou se o comando não foi realmente executado com sucesso.

## Estilo de código

- Prefira nomes claros a comentários que apenas repetem o código.
- Use comentários em português para contratos não óbvios e decisões específicas do domínio.
- Não mantenha código morto, funcionalidades futuras vazias ou abstrações especulativas.
- Trate avisos relevantes do compilador e do lint antes de entregar uma etapa.
