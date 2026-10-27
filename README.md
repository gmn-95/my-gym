# my-gym

Aplicativo Android nativo e offline-first para organizar fichas de musculação e executar sessões com cronômetros persistentes.

## Estado atual

- Kotlin e Jetpack Compose com Material 3.
- Tema claro, escuro ou seguindo o sistema, com preferência local.
- Idioma configurável entre português brasileiro, inglês ou idioma do dispositivo.
- Textos iniciais em português brasileiro e inglês.
- Room/SQLite com exportação do esquema habilitada.
- Fichas que agrupam múltiplos treinos, com criação, edição, duplicação e exclusão confirmada dos treinos.
- Exercícios manuais reutilizáveis em diferentes treinos.
- Busca sob demanda na wger, limitada a resultados com tradução em português, com vídeo ou imagem remotos quando disponíveis.
- Séries, repetições fixas ou em faixa e descanso em minutos/segundos.
- Reordenação e remoção dos exercícios de um treino.
- Sessão com snapshot do treino, conclusão individual de cada série e apenas um treino ativo por vez.
- Duração média real de cada treino, baseada apenas em sessões concluídas.
- Calendário mensal, lista e detalhe local de sessões concluídas, com duração, exercícios concluídos e remoção individual confirmada.
- Backup JSON por seletor de arquivos Android, com validação e restauração transacional.
- Cronômetro de tempo ativo com pausa explícita.
- Descanso independente com iniciar, pausar, continuar, reiniciar e encerrar.
- Recuperação da sessão após recriação da tela ou encerramento do processo.
- `applicationId` definitivo: `com.gmn95.mygym`.
- Versão atual: `versionCode 14` / `versionName 0.13.0`.
- `minSdk 26`, `targetSdk 37` e `compileSdk 37.0`.

As sessões persistidas alimentam o calendário, a lista de histórico e a duração média dos treinos.

## Uso atual

1. Na tela **Minhas fichas**, use **Nova ficha** e informe um nome, como `Rotina`.
2. Abra a ficha, use **Adicionar treino** e informe um nome, como `A — Superiores`.
3. Abra o treino e use **Adicionar exercício**.
4. Crie um exercício manual com nome e observações opcionais, ou reutilize um exercício já cadastrado.
5. Como alternativa, toque em **Buscar no catálogo wger**, pesquise pelo nome e abra um resultado para consultar vídeo ou imagem antes de adicioná-lo. A busca mostra somente itens que tenham tradução em português.
6. Use **Editar** para definir séries, repetições e descanso, e **Subir** e **Descer** para alterar a ordem.
7. Um treino pode ser aberto, duplicado ou excluído dentro da ficha. A exclusão sempre exige confirmação.
8. Use **Iniciar treino** para criar uma sessão baseada em um snapshot do treino.
9. Durante o treino, use **Mostrar séries** para abrir os detalhes de um exercício. Ao marcar uma série, o descanso configurado começa automaticamente; também é possível marcar ou desmarcar todas as séries pelo controle **Exercício concluído**.
10. Após concluir treinos, a ficha passa a mostrar a duração média ativa de cada treino. Sessões canceladas não entram nesse cálculo.
11. Use **Histórico** na tela inicial para consultar as sessões concluídas e toque em uma delas para ver os exercícios do snapshot, mesmo que o treino original tenha sido excluído.
12. Use **Calendário** para navegar pelos meses; dias destacados possuem sessões concluídas e podem ser selecionados.
13. Use **Finalizar treino** para manter a sessão concluída ou **Cancelar treino** para encerrá-la sem tratá-la futuramente como treino realizado.
14. Em **Backup**, exporte os dados para JSON antes de reinstalar ou restaurar o app. A importação valida o arquivo, mostra um resumo e substitui os dados somente após confirmação.
15. Em **Configurações**, escolha o tema e o idioma. As preferências ficam no Room e também são incluídas no backup JSON.

## Catálogo wger

O catálogo usa os endpoints públicos da [wger](https://wger.readthedocs.io/en/latest/api/api.html) somente em buscas iniciadas pelo usuário. A API publica o idioma `pt` (Português), sem uma variante independente `pt-BR`; por isso o app filtra cada resposta para manter somente traduções portuguesas. A pesquisa é feita pelo nome e a paginação segue o endereço devolvido pela própria API.

O app não grava dados, descrições, instruções, imagens ou vídeos remotos no Room, no backup JSON ou no cache de disco. Ao adicionar um resultado, salva apenas o nome escolhido como exercício local para que o treino continue funcionando offline. A mídia permanece remota e pode não estar disponível sem conexão. Quando a API informar licença e autor da mídia, essa atribuição é mostrada nos detalhes do exercício.

## Comportamento dos cronômetros

- Bloquear a tela ou trocar de aplicativo não pausa a sessão nem o descanso.
- Os tempos são calculados com o relógio monotônico do Android; não dependem de um contador que precisa permanecer visível.
- Ao colocar o app em segundo plano, os valores são checkpointados no Room sem pausar os cronômetros.
- Se o aparelho for reiniciado, uma sessão e um descanso que estavam ativos são recuperados pausados no último checkpoint. O período com o aparelho desligado não é contado.
- O cronômetro de descanso é independente da pausa da sessão.
- A conclusão total de um exercício é derivada: ele só aparece concluído depois que todas as suas séries forem marcadas.

## Aparência e idioma

O tema pode seguir o sistema ou ser fixado como claro/escuro. O idioma pode seguir o dispositivo ou ser fixado como português brasileiro/inglês; a troca atualiza os recursos localizados do app e é armazenada nas preferências Room.

## Pré-requisitos no Ubuntu/Linux

1. JDK 17 ou mais recente. O Android Gradle Plugin requer no mínimo o JDK 17.
2. Android Studio compatível com o Android Gradle Plugin 9.4, ou Android SDK instalado por linha de comando.
3. No SDK Manager, instalar:
   - Android SDK Platform 37.0;
   - Android SDK Build-Tools 36.0.0 ou mais recente compatível;
   - Android SDK Platform-Tools;
   - Android Emulator, caso queira usar um dispositivo virtual.

Se o SDK estiver fora do local padrão, crie um arquivo `local.properties`, que não deve ser versionado:

```properties
sdk.dir=/caminho/para/Android/Sdk
```

Alternativamente, configure `ANDROID_HOME` no shell.

## Abrir e executar no Android Studio

1. Abra a pasta raiz deste repositório no Android Studio.
2. Aguarde a sincronização do Gradle e a instalação das dependências.
3. Selecione um emulador ou um celular com depuração USB habilitada.
4. Execute a configuração `app` pelo botão **Run**.

## Compilar pela linha de comando

O Gradle Wrapper baixa a versão correta do Gradle automaticamente:

```bash
./gradlew assembleDebug
```

O APK de debug será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Para instalar ou atualizar o APK de debug em um aparelho conectado:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Não desinstale o aplicativo antes de uma atualização se quiser preservar seus dados locais.

## Gerar e atualizar uma versão release assinada

Para uso contínuo no seu aparelho, gere APKs release sempre com a **mesma chave pessoal**. Crie-a uma única vez, fora deste repositório, e guarde a chave e as senhas em local seguro:

```bash
mkdir -p "$HOME/.android/my-gym"
keytool -genkeypair -v \
  -keystore "$HOME/.android/my-gym/my-gym-release.jks" \
  -alias my-gym \
  -keyalg RSA -keysize 2048 -validity 10000
```

Na raiz do projeto, crie `keystore.properties`. Esse arquivo e arquivos `.jks` já estão no `.gitignore`; nunca os envie ao Git:

```properties
storeFile=/caminho/absoluto/para/my-gym-release.jks
storePassword=sua-senha-do-cofre
keyAlias=my-gym
keyPassword=sua-senha-da-chave
```

Com a configuração local pronta, gere o APK:

```bash
./gradlew assembleRelease
```

O arquivo será criado em `app/build/outputs/apk/release/app-release.apk`. Para instalar ou atualizar um release em um aparelho conectado:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Em instalação manual, copie o APK ao celular, abra-o por um gerenciador de arquivos e autorize a instalação para o aplicativo que abriu o arquivo quando o Android solicitar.

O Android só aceita atualização direta quando o `applicationId`, a assinatura e o `versionCode` forem compatíveis. Antes de cada nova distribuição, aumente o `versionCode` em `app/build.gradle.kts`; não altere `com.gmn95.mygym` nem troque a keystore. Um APK debug e um APK release normalmente têm assinaturas diferentes: não é possível usar um para atualizar diretamente o outro. Para migrar de debug para release, exporte o backup JSON, desinstale o debug, instale o primeiro release e importe o backup. A desinstalação remove os dados locais.

## Testes e verificações

Os comandos previstos para a etapa inicial são:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleDebugAndroidTest
```

Os testes unitários validam as configurações dos exercícios e os cálculos monotônicos dos cronômetros. Os testes instrumentados validam as migrações Room, os snapshots por série e os fluxos transacionais de fichas, sessões, pausas, descansos e recuperação após reinicialização. Para executá-los, inicie um emulador ou conecte um celular e use:

```bash
./gradlew connectedDebugAndroidTest
```

## Persistência e futuras atualizações

O banco `my-gym.db` está na versão 5. A migração `1 → 2` cria os treinos e exercícios; a migração `2 → 3` cria sessões, snapshots e o estado do descanso; a migração `3 → 4` cria as séries individuais para os snapshots já existentes; a migração `4 → 5` cria as fichas e coloca cada treino existente em uma ficha com o mesmo nome. Todas preservam os dados anteriores. Os esquemas gerados pelo Room ficam em `app/schemas/` e são versionados. Toda mudança futura do esquema deverá:

1. aumentar a versão do banco;
2. fornecer uma migração Room explícita;
3. preservar os dados existentes;
4. incluir um teste de migração.

Não será usado `fallbackToDestructiveMigration()` em builds do aplicativo. O `applicationId` `com.gmn95.mygym` também não poderá ser alterado. Atualizações release precisarão usar sempre a mesma chave de assinatura e um `versionCode` maior.

## Backup JSON

O backup usa o seletor de documentos do Android e não solicita permissão ampla de armazenamento. O formato atual tem `schemaVersion: 2` e inclui fichas, treinos, exercícios manuais, configurações, sessões, snapshots das séries e preferências. Backups no formato 1 continuam sendo aceitos, com cada treino restaurado em sua própria ficha. A importação substitui os dados locais após confirmação; mesclagem ainda não está disponível. A leitura, validação e gravação ocorrem antes/ dentro de uma única transação para evitar banco parcialmente restaurado. Sessões e descansos que estavam ativos são salvos como pausados, com os tempos calculados no momento da exportação.

## Estrutura

```text
app/src/main/java/com/gmn95/mygym/
├── data/local/       banco Room, entidades, DAOs e migrações
├── data/repository/  contrato e implementação transacional dos dados
├── domain/           validações puras das configurações
├── feature/          ViewModels e estados de tela
├── ui/               telas e raiz da interface Compose
└── ui/theme/         cores e tema Material 3
```

As decisões e convenções obrigatórias para as próximas etapas estão em `AGENTS.md`. A especificação completa está em `SPEC.md`.
