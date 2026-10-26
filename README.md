# my-gym

Aplicativo Android nativo e offline-first para organizar e executar fichas de musculação. O projeto está na etapa inicial: possui a estrutura Gradle, uma tela Compose simples e o banco Room preparado para evoluir com migrações explícitas.

## Estado atual

- Kotlin e Jetpack Compose com Material 3.
- Tema claro ou escuro seguindo o sistema.
- Textos iniciais em português brasileiro e inglês.
- Room/SQLite com exportação do esquema habilitada.
- `applicationId` definitivo: `com.gmn95.mygym`.
- `minSdk 26`, `targetSdk 37` e `compileSdk 37.0`.

Criação de fichas, execução de sessões, histórico, backup e integração com a ExerciseDB ainda não fazem parte desta etapa.

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

## Testes e verificações

Os comandos previstos para a etapa inicial são:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Testes de banco, migração e interface serão acrescentados juntamente com as regras que eles validam.

## Persistência e futuras atualizações

O banco `my-gym.db` começa na versão 1. Os esquemas gerados pelo Room ficam em `app/schemas/` e devem ser versionados. Toda mudança futura do esquema deverá:

1. aumentar a versão do banco;
2. fornecer uma migração Room explícita;
3. preservar os dados existentes;
4. incluir um teste de migração.

Não será usado `fallbackToDestructiveMigration()` em builds do aplicativo. O `applicationId` `com.gmn95.mygym` também não poderá ser alterado. Atualizações release precisarão usar sempre a mesma chave de assinatura e um `versionCode` maior.

## Estrutura

```text
app/src/main/java/com/gmn95/mygym/
├── data/local/    banco Room, entidades e DAOs
├── ui/            raiz da interface Compose
└── ui/theme/      cores e tema Material 3
```

As decisões e convenções obrigatórias para as próximas etapas estão em `AGENTS.md`. A especificação completa está em `SPEC.md`.
