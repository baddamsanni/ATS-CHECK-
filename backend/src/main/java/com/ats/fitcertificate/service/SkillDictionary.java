package com.ats.fitcertificate.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Small stopword + skills dictionary used by the keyword extractor. */
@Component
public class SkillDictionary {

    static final Set<String> STOPWORDS = Set.of(
            // generic english stopwords
            "a","an","the","and","or","but","if","then","else","for","of","to","in","on","at","by",
            "with","without","into","from","as","is","are","was","were","be","been","being","have",
            "has","had","do","does","did","will","would","should","could","can","may","might","must",
            "this","that","these","those","it","its","they","them","their","there","here","we","you",
            "your","our","us","i","me","my","he","she","his","her","who","whom","which","what","when",
            "where","why","how","all","any","both","each","few","more","most","other","some","such",
            "no","nor","not","only","own","same","so","than","too","very","just","also","about","above",
            "after","again","against","before","below","between","during","further","once","up","down",
            "out","off","over","under","through","while","well","able","plus","highly","deep","solid",
            "proven","track","record","passion","passionate","self","motivated","motivation",
            "opportunity","grow","growth","career","people","culture","values","value","mission",
            "vision","impact","deliver","delivering","deliveries","stakeholder","stakeholders",
            "partner","partners","partnering","cross","functional",
            // job-description boilerplate
            "job","description","role","position","team","company","candidate","candidates",
            "applicant","applicants","hire","hiring","looking","seeking","join",
            "work","working","worked","experience","experiences","experienced","skills","skill",
            "ability","abilities","responsibilities","responsibility","requirements","requirement",
            "required","require","preferred","nice","strong","excellent","good","great","years",
            "year","yrs","etc","e.g","i.e","including","include","includes","included","across",
            "within","via","per","new","one","two","three","using","use","used","uses",
            "help","helps","helping","ensure","ensures","ensuring","drive","driving","build",
            "building","engineering","engineer","engineers","developer","developers","development",
            "develop","developing","design","designing","designed","designs","implement",
            "implementation","implementing","implemented","maintain","maintaining","maintained",
            "support","supporting","supported","supports","create","creating","created","creates",
            "lead","leading","led","leads","manage","managing","managed","manages","management",
            "manager","owners","ownership","owning","responsible","responsibly"
    );

    /** Common tech skills used to boost keyword extraction (lowercased). */
    static final Set<String> SKILLS = Set.of(
            "java","kotlin","scala","groovy","python","ruby","php","go","golang","rust","c","c++",
            "c#","javascript","typescript","js","ts","node","nodejs","deno","bun","react","reactjs",
            "redux","next","nextjs","vue","vuejs","angular","svelte","ember","backbone","jquery",
            "html","css","sass","scss","tailwind","bootstrap","materialui","mui","webpack","vite",
            "babel","eslint","jest","cypress","playwright","selenium","testing","tests","junit",
            "mockito","pytest","test","spring","springboot","spring-boot","springcloud","quarkus",
            "micronaut","django","flask","fastapi","rails","express","expressjs","nestjs","laravel",
            "symfony","dotnet",".net","asp","aspnet","graphql","grpc","rest","restful","soap",
            "websocket","websockets","api","apis","microservices","microservice","serverless",
            "lambda","containers","container","docker","kubernetes","k8s","openshift","helm","istio",
            "envoy","terraform","ansible","puppet","chef","cloudformation","pulumi","aws","gcp",
            "azure","azuredevops","ec2","s3","rds","eks","ecs","fargate","cloudwatch","iam","vpc",
            "route53","cloudfront","sqs","sns","kinesis","dynamodb","aurora","redshift","athena",
            "glue","emr","bigquery","snowflake","databricks","spark","hadoop","hive","presto",
            "trino","airflow","kafka","rabbitmq","activemq","nats","redis","memcached",
            "elasticsearch","opensearch","solr","mongodb","postgres","postgresql","mysql",
            "mariadb","oracle","sqlserver","mssql","cassandra","couchbase","neo4j","graphdb",
            "janusgraph","etl","elt","datawarehouse","datamart","datalake","lakehouse","dbt",
            "looker","tableau","powerbi","superset","metabase","grafana","kibana","prometheus",
            "datadog","splunk","newrelic","opentelemetry","jaeger","zipkin","ci","cd","ci/cd",
            "jenkins","circleci","gitlab","bitbucket","github","actions","argo","argocd","flux",
            "vault","consul","linux","unix","bash","shell","powershell","nginx","haproxy",
            "apache","tomcat","jetty","netty","vertx","akka","zeromq","finagle","protobuf",
            "avro","thrift","json","yaml","toml","xml","sql","nosql","newsql","oltp","olap","htap",
            "machine","learning","ml","ai","deeplearning","tensorflow","pytorch","keras","scikit",
            "scikit-learn","pandas","numpy","scipy","nlp","llm","rag","transformers","huggingface",
            "opencv","computer","vision","reinforcement","xgboost","lightgbm","catboost","sparkml",
            "agile","scrum","kanban","safe","xp","tdd","bdd","ddd","solid","clean","architecture",
            "architectural","system","systems","distributed","scalability","scalable",
            "reliability","reliable","observability","security","oauth","oidc","saml","jwt","rbac",
            "abac","pki","tls","ssl","encryption","kms","hsm","gdpr","hipaa","pci","soc2",
            "iso27001","compliance","fintech","healthcare","ecommerce","retail","b2b","b2c","saas",
            "paas","iaas","onprem","mobile","ios","android","swift","flutter","reactnative",
            "xamarin","gradle","maven","ant","sbt","npm","yarn","pnpm","pip","poetry","conda",
            "make","cmake","bazel"
    );

    /** Canonical display casing for common skill keywords (lowercase key -> proper form). */
    static final Map<String, String> CANONICAL_CASE = Map.ofEntries(
            Map.entry("js", "JavaScript"), Map.entry("javascript", "JavaScript"),
            Map.entry("ts", "TypeScript"), Map.entry("typescript", "TypeScript"),
            Map.entry("aws", "AWS"), Map.entry("gcp", "GCP"), Map.entry("azure", "Azure"),
            Map.entry("node", "Node.js"), Map.entry("nodejs", "Node.js"),
            Map.entry("react", "React"), Map.entry("reactjs", "React"),
            Map.entry("vue", "Vue.js"), Map.entry("vuejs", "Vue.js"), Map.entry("angular", "Angular"),
            Map.entry("html", "HTML"), Map.entry("css", "CSS"), Map.entry("sql", "SQL"),
            Map.entry("nosql", "NoSQL"), Map.entry("graphql", "GraphQL"),
            Map.entry("rest", "REST APIs"), Map.entry("restful", "REST APIs"),
            Map.entry("api", "APIs"), Map.entry("apis", "APIs"),
            Map.entry("k8s", "Kubernetes"), Map.entry("kubernetes", "Kubernetes"),
            Map.entry("docker", "Docker"), Map.entry("ci", "CI/CD"), Map.entry("cd", "CI/CD"),
            Map.entry("ci/cd", "CI/CD"), Map.entry("postgres", "PostgreSQL"),
            Map.entry("postgresql", "PostgreSQL"), Map.entry("mongodb", "MongoDB"),
            Map.entry("mysql", "MySQL"), Map.entry("redis", "Redis"), Map.entry("kafka", "Kafka"),
            Map.entry("microservices", "Microservices"), Map.entry("microservice", "Microservices"),
            Map.entry("oauth", "OAuth2"), Map.entry("jwt", "JWT"), Map.entry("junit", "JUnit"),
            Map.entry("mockito", "Mockito"), Map.entry("dotnet", ".NET"), Map.entry(".net", ".NET"),
            Map.entry("asp", "ASP.NET"), Map.entry("aspnet", "ASP.NET"),
            Map.entry("c#", "C#"), Map.entry("c++", "C++"), Map.entry("golang", "Go"),
            Map.entry("go", "Go"), Map.entry("kotlin", "Kotlin"), Map.entry("swift", "Swift"),
            Map.entry("spring", "Spring"), Map.entry("springboot", "Spring Boot"),
            Map.entry("spring-boot", "Spring Boot"), Map.entry("django", "Django"),
            Map.entry("flask", "Flask"), Map.entry("fastapi", "FastAPI"), Map.entry("rails", "Rails"),
            Map.entry("express", "Express.js"), Map.entry("expressjs", "Express.js"),
            Map.entry("nestjs", "NestJS"), Map.entry("laravel", "Laravel"),
            Map.entry("terraform", "Terraform"), Map.entry("ansible", "Ansible"),
            Map.entry("jenkins", "Jenkins"), Map.entry("gitlab", "GitLab"), Map.entry("github", "GitHub"),
            Map.entry("elasticsearch", "Elasticsearch"), Map.entry("grafana", "Grafana"),
            Map.entry("prometheus", "Prometheus"), Map.entry("tensorflow", "TensorFlow"),
            Map.entry("pytorch", "PyTorch"), Map.entry("android", "Android"), Map.entry("ios", "iOS"),
            Map.entry("flutter", "Flutter"), Map.entry("gradle", "Gradle"), Map.entry("maven", "Maven"),
            Map.entry("python", "Python"), Map.entry("java", "Java"), Map.entry("scala", "Scala"),
            Map.entry("ruby", "Ruby"), Map.entry("php", "PHP"), Map.entry("linux", "Linux"),
            Map.entry("bash", "Bash"), Map.entry("agile", "Agile"), Map.entry("scrum", "Scrum"),
            Map.entry("tdd", "TDD"), Map.entry("ddd", "DDD"), Map.entry("solid", "SOLID"),
            Map.entry("saas", "SaaS"), Map.entry("mvvm", "MVVM"), Map.entry("mvc", "MVC")
    );

    /** Returns the properly-cased display form of a keyword, or Title Case as a fallback. */
    public static String canonicalCase(String keyword) {
        if (keyword == null || keyword.isBlank()) return keyword;
        String lower = keyword.toLowerCase().trim();
        if (CANONICAL_CASE.containsKey(lower)) return CANONICAL_CASE.get(lower);
        // Title case each word for unknown multi-word terms.
        String[] words = lower.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    /**
     * Extract candidate requirement keywords from a JD: tokens that are
     * either in the SKILLS set or are non-stopword alphabetic tokens of
     * length ≥3. Returns a de-duplicated, order-preserving list.
     */
    public List<String> extractKeywords(String jd) {
        if (jd == null || jd.isBlank()) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        String[] tokens = jd.toLowerCase().split("[^a-z0-9+#.-]+");
        for (String t : tokens) {
            if (t.isBlank()) continue;
            if (SKILLS.contains(t)) { out.add(t); continue; }
            if (STOPWORDS.contains(t)) continue;
            if (t.length() < 3) continue;
            if (t.matches("[0-9.-]+")) continue;
            out.add(t);
        }
        return new ArrayList<>(out);
    }
}
