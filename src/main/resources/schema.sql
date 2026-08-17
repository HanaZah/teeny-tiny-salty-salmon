-- =============================================
-- 1. GEOGRAPHY (CITIES & STREETS)
-- =============================================

CREATE TABLE IF NOT EXISTS ADDRESSES (
                           ADDRESS_ID         INTEGER NOT NULL,
                           POSTAL_CODE        VARCHAR2(6 CHAR) NOT NULL,
                           CITY               VARCHAR2(100 CHAR) NOT NULL,
                           HOUSE_NUMBER       VARCHAR2(10 CHAR) NOT NULL,
                           STREET             VARCHAR2(100 CHAR) NOT NULL,
                           CONSTRAINT ADDRESS_PK PRIMARY KEY (ADDRESS_ID),
                           CONSTRAINT ADDRESS_POSTAL_CODE_CK CHECK (REGEXP_LIKE(POSTAL_CODE, '^\d{3}\s\d{2}$')),
                           CONSTRAINT CHK_HOUSE_NUMBER_FORMAT
                                CHECK (REGEXP_LIKE(HOUSE_NUMBER, '^[1-9][0-9]{0,3}(/[1-9][0-9]{0,3}[a-z]?)?$'))
);
/

-- =============================================
-- 2. CORE DOMAIN (USERS & CLIENTS)
-- =============================================

CREATE TABLE IF NOT EXISTS USERS (
                       USER_ID          INTEGER NOT NULL,
                       EMPLOYEE_ID      VARCHAR2(20 CHAR) NOT NULL UNIQUE, -- For login
                       PASSWORD_HASH    VARCHAR2(255 CHAR) NOT NULL,
                       ICO              VARCHAR2(8 CHAR),
                       FIRST_NAME       VARCHAR2(50 CHAR) NOT NULL,
                       LAST_NAME        VARCHAR2(50 CHAR) NOT NULL,
                       USER_TYPE        VARCHAR2(20 CHAR) NOT NULL,
                       PHONE            VARCHAR2(20 CHAR) NOT NULL,
                       EMAIL            VARCHAR2(254 CHAR) NOT NULL,
                       IS_ACTIVE        NUMBER(1) DEFAULT 1 NOT NULL,
                       VERSION          INTEGER DEFAULT 0 NOT NULL,
                       CONSTRAINT USER_PK PRIMARY KEY (USER_ID),
                       CONSTRAINT USER_ID_TYPE_UN UNIQUE (USER_ID, USER_TYPE),
                       CONSTRAINT CHK_USER_TYPE CHECK (USER_TYPE IN ('ADMIN', 'ADVISOR')),
                       CONSTRAINT CHK_USER_ICO CHECK (REGEXP_LIKE(ICO, '^\d{8}$')),
                       CONSTRAINT CHK_USER_ACTIVE CHECK (IS_ACTIVE IN (0, 1))
);
/

CREATE TABLE IF NOT EXISTS CLIENTS (
                        CLIENT_ID               INTEGER NOT NULL,
                        CLIENT_UID              VARCHAR2(20 CHAR) NOT NULL UNIQUE,
                        PERSONAL_ID             VARCHAR2(10 CHAR) NOT NULL,
                        BIRTH_DATE              DATE NOT NULL,
                        LAST_NAME               VARCHAR2(50 CHAR) NOT NULL,
                        FIRST_NAME              VARCHAR2(50 CHAR) NOT NULL,
                        OCCUPATION              VARCHAR2(100 CHAR) NOT NULL,
                        PHONE                   VARCHAR2(20 CHAR) NOT NULL,
                        EMAIL                   VARCHAR2(254 CHAR) NOT NULL,
                        ID_CARD_NUMBER          VARCHAR2(9 CHAR) NOT NULL,
                        ID_CARD_ISSUE_DATE      DATE NOT NULL,
                        ID_CARD_EXPIRY_DATE     DATE NOT NULL,
                        ID_CARD_ISSUER          VARCHAR2(100 CHAR) NOT NULL,
                        LAST_UPDATE             DATE NOT NULL,
                        ADVISOR_ID              INTEGER NOT NULL,
                        RESIDENT_ADDRESS_ID     INTEGER NOT NULL,
                        CONTACT_ADDRESS_ID      INTEGER NOT NULL,
                        VERSION                 INTEGER DEFAULT 0 NOT NULL,
                        IS_ACTIVE               NUMBER(1) DEFAULT 1 NOT NULL,
                        CONSTRAINT CLIENT_PK PRIMARY KEY (CLIENT_ID),
                        CONSTRAINT CLIENT_PERSONAL_ID_UN UNIQUE (PERSONAL_ID),
                        CONSTRAINT CLIENT_ID_CARD_UN UNIQUE (ID_CARD_NUMBER),
                        CONSTRAINT CLIENT_PERSONAL_ID_CK CHECK (REGEXP_LIKE(PERSONAL_ID, '^[[:digit:]]{10}$')),
                        CONSTRAINT CLIENT_ID_CARD_CK CHECK (REGEXP_LIKE(ID_CARD_NUMBER, '^[[:digit:]]{9}$')),
                        CONSTRAINT CLIENT_EXPIRY_CK CHECK (ID_CARD_EXPIRY_DATE > ID_CARD_ISSUE_DATE),
                        CONSTRAINT CLIENT_USER_FK FOREIGN KEY (ADVISOR_ID) REFERENCES USERS (USER_ID),
                        CONSTRAINT CLIENT_RES_ADDR_FK FOREIGN KEY (RESIDENT_ADDRESS_ID) REFERENCES ADDRESSES (ADDRESS_ID),
                        CONSTRAINT CLIENT_CONT_ADDR_FK FOREIGN KEY (CONTACT_ADDRESS_ID) REFERENCES ADDRESSES (ADDRESS_ID),
                        CONSTRAINT CLIENT_ACTIVE_CK CHECK (IS_ACTIVE IN (0, 1))
    );
/

-- =============================================
-- 3. BUDGET (INCOMES & EXPENSES)
-- =============================================

CREATE TABLE IF NOT EXISTS INCOME_TYPES (
                              INCOME_TYPE_ID  INTEGER NOT NULL,
                              NAME            VARCHAR2(50 CHAR) NOT NULL,
                              CONSTRAINT INCOME_TYPE_PK PRIMARY KEY (INCOME_TYPE_ID),
                              CONSTRAINT INCOME_TYPE_NAME_UN UNIQUE (NAME)
);
/

CREATE TABLE IF NOT EXISTS INCOMES (
                         INCOME_ID      INTEGER NOT NULL,
                         AMOUNT         INTEGER NOT NULL,
                         CLIENT_ID      INTEGER NOT NULL,
                         INCOME_TYPE_ID INTEGER NOT NULL,
                         CONSTRAINT INCOME_PK PRIMARY KEY (INCOME_ID),
                         CONSTRAINT INCOME_AMOUNT_CK CHECK (AMOUNT BETWEEN 1 AND 999999999),
                         CONSTRAINT INCOME_CLIENT_FK FOREIGN KEY (CLIENT_ID) REFERENCES CLIENTS (CLIENT_ID),
                         CONSTRAINT INCOME_TYPE_FK FOREIGN KEY (INCOME_TYPE_ID) REFERENCES INCOME_TYPES (INCOME_TYPE_ID),
                         CONSTRAINT INCOME_CLIENT_TYPE_UN UNIQUE (CLIENT_ID, INCOME_TYPE_ID)
);
/

CREATE TABLE IF NOT EXISTS EXPENSE_TYPES (
                               EXPENSE_TYPE_ID INTEGER NOT NULL,
                               NAME            VARCHAR2(50 CHAR) NOT NULL,
                               CONSTRAINT EXPENSE_TYPE_PK PRIMARY KEY (EXPENSE_TYPE_ID),
                               CONSTRAINT EXPENSE_TYPE_NAME_UN UNIQUE (NAME)
);
/

CREATE TABLE IF NOT EXISTS EXPENSES (
                          EXPENSE_ID      INTEGER NOT NULL,
                          AMOUNT          INTEGER NOT NULL,
                          IS_MANDATORY    NUMBER(1) DEFAULT 0 NOT NULL,
                          CLIENT_ID       INTEGER NOT NULL,
                          EXPENSE_TYPE_ID INTEGER NOT NULL,
                          CONSTRAINT EXPENSE_PK PRIMARY KEY (EXPENSE_ID),
                          CONSTRAINT EXPENSE_AMOUNT_CK CHECK (AMOUNT BETWEEN 1 AND 999999999),
                          CONSTRAINT EXPENSE_MANDATORY_CK CHECK (IS_MANDATORY IN (0, 1)),
                          CONSTRAINT EXPENSE_CLIENT_FK FOREIGN KEY (CLIENT_ID) REFERENCES CLIENTS (CLIENT_ID),
                          CONSTRAINT EXPENSE_TYPE_FK FOREIGN KEY (EXPENSE_TYPE_ID) REFERENCES EXPENSE_TYPES (EXPENSE_TYPE_ID),
                          CONSTRAINT EXPENSE_CLIENT_TYPE_UN UNIQUE (CLIENT_ID, EXPENSE_TYPE_ID)
);
/

-- =============================================
-- 4. PRODUCTS & PROVIDERS
-- =============================================

CREATE TABLE IF NOT EXISTS PROVIDERS (
                           PROVIDER_ID INTEGER NOT NULL,
                           NAME        VARCHAR2(100 CHAR) NOT NULL,
                           CONSTRAINT PROVIDER_PK PRIMARY KEY (PROVIDER_ID),
                           CONSTRAINT PROVIDER_NAME_UN UNIQUE (NAME)
);
/

CREATE TABLE IF NOT EXISTS PRODUCT_TYPES (
                               PRODUCT_TYPE_ID INTEGER NOT NULL,
                               NAME            VARCHAR2(50 CHAR) NOT NULL,
                               CONSTRAINT PRODUCT_TYPE_PK PRIMARY KEY (PRODUCT_TYPE_ID),
                               CONSTRAINT PRODUCT_TYPE_NAME_UN UNIQUE (NAME)
);
/

CREATE TABLE IF NOT EXISTS PRODUCTS (
                  PRODUCT_ID         INTEGER NOT NULL,
                  NAME               VARCHAR2(150 CHAR) NOT NULL,
                  AMOUNT             NUMBER(10,2) NOT NULL,
                  START_DATE         DATE NOT NULL,
                  END_DATE           DATE,
                  PRODUCT_TYPE_ID    INTEGER NOT NULL,
                  CLIENT_ID          INTEGER NOT NULL,
                  PROVIDER_ID        INTEGER NOT NULL,
                  ADVISOR_ID         INTEGER,
                  CONSTRAINT PRODUCT_PK PRIMARY KEY (PRODUCT_ID),
                  CONSTRAINT PRODUCT_AMOUNT_CK CHECK (AMOUNT BETWEEN 0.00 AND 99999999.99),
                  CONSTRAINT PRODUCT_DATES_CK CHECK (END_DATE IS NULL OR END_DATE > START_DATE),
                  CONSTRAINT PRODUCT_CLIENT_FK FOREIGN KEY (CLIENT_ID) REFERENCES CLIENTS (CLIENT_ID),
                  CONSTRAINT PRODUCT_TYPE_FK FOREIGN KEY (PRODUCT_TYPE_ID) REFERENCES PRODUCT_TYPES (PRODUCT_TYPE_ID),
                  CONSTRAINT PRODUCT_PROVIDER_FK FOREIGN KEY (PROVIDER_ID) REFERENCES PROVIDERS (PROVIDER_ID),
                  CONSTRAINT PRODUCT_USER_FK FOREIGN KEY (ADVISOR_ID) REFERENCES USERS (USER_ID)
);
/

-- =============================================
-- 6. VIEWS
-- =============================================

CREATE OR REPLACE VIEW V_CLIENT_SEARCH_MINIMAL AS
SELECT
    C.CLIENT_UID,
    U.EMPLOYEE_ID AS ADVISOR_EMPLOYEE_ID,
    C.PERSONAL_ID,
    C.FIRST_NAME || ' ' || C.LAST_NAME AS FULL_NAME,
    A_C.CITY AS CONTACT_CITY_NAME,
    A_C.POSTAL_CODE AS CONTACT_POSTAL_CODE,
    C.IS_ACTIVE
FROM CLIENTS C
         JOIN USERS U ON C.ADVISOR_ID = U.USER_ID
         JOIN ADDRESSES A_C ON C.CONTACT_ADDRESS_ID = A_C.ADDRESS_ID;
/

-- =============================================
-- 8. TRIGGERS (BUSINESS LOGIC)
-- =============================================

CREATE OR REPLACE TRIGGER TRG_CLIENTS_BIU
BEFORE INSERT OR UPDATE ON CLIENTS
FOR EACH ROW
DECLARE
    v_adv_active NUMBER(1);
    v_usr_role VARCHAR2(50 CHAR);
BEGIN
    :NEW.LAST_UPDATE := COALESCE(:NEW.LAST_UPDATE, TRUNC(SYSDATE));
    IF :NEW.ID_CARD_ISSUE_DATE <= :NEW.BIRTH_DATE THEN
        raise_application_error(-20001, 'Client ID card issue date cannot precede or match birth date');
    END IF;
    IF :NEW.ID_CARD_EXPIRY_DATE < TRUNC(SYSDATE) OR :NEW.ID_CARD_EXPIRY_DATE <= :NEW.ID_CARD_ISSUE_DATE THEN
        raise_application_error(-20002, 'Client ID card expired or the expiry date is invalid (not after issue date).');
    END IF;
    IF :NEW.ID_CARD_ISSUE_DATE > TRUNC(SYSDATE) THEN
        raise_application_error(-20003, 'Client ID card issue date cannot be in the future.');
    END IF;
    IF INSERTING OR (UPDATING AND :NEW.ADVISOR_ID <> :OLD.ADVISOR_ID) THEN
        SELECT IS_ACTIVE, USER_TYPE INTO v_adv_active, v_usr_role FROM USERS WHERE USER_ID = :NEW.ADVISOR_ID;
        IF v_usr_role <> 'ADVISOR' THEN
           raise_application_error(-20004, 'The assigned user must be an advisor.');
        END IF;
        IF v_adv_active = 0 THEN
            raise_application_error(-20005, 'Cannot assign an inactive advisor to a client.');
        END IF;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_INCOMES_BIU
BEFORE INSERT OR UPDATE ON INCOMES
FOR EACH ROW
DECLARE
    v_client_active NUMBER(1);
BEGIN
    IF INSERTING OR (UPDATING AND :NEW.CLIENT_ID <> :OLD.CLIENT_ID) THEN
        SELECT IS_ACTIVE INTO v_client_active FROM CLIENTS WHERE CLIENT_ID = :NEW.CLIENT_ID;
        IF v_client_active = 0 THEN
            raise_application_error(-20006, 'Cannot add an income to an inactive client.');
        END IF;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_PRODUCTS_BIU
BEFORE INSERT OR UPDATE ON PRODUCTS
FOR EACH ROW
DECLARE
    v_expiry DATE;
    v_current_adv INTEGER;
    v_client_active NUMBER(1);
    v_manager_active NUMBER(1);
BEGIN
    IF INSERTING THEN
        SELECT ID_CARD_EXPIRY_DATE, ADVISOR_ID, IS_ACTIVE
        INTO v_expiry, v_current_adv, v_client_active
        FROM CLIENTS WHERE CLIENT_ID = :NEW.CLIENT_ID;

        IF v_client_active = 0 THEN
            raise_application_error(-20007, 'Cannot arrange product: Client is inactive.');
        END IF;
        IF v_expiry < TRUNC(SYSDATE) THEN
            raise_application_error(-20008, 'Cannot arrange product: Client ID card expired.');
        END IF;
        IF :NEW.ADVISOR_ID IS NOT NULL THEN
            IF v_current_adv <> :NEW.ADVISOR_ID THEN
                raise_application_error(-20009, 'The arranger must be the client current advisor.');
            END IF;

            SELECT IS_ACTIVE INTO v_manager_active FROM USERS WHERE USER_ID = :NEW.ADVISOR_ID;
            IF v_manager_active = 0 THEN
                 raise_application_error(-20010, 'Cannot assign an inactive advisor to manage a product.');
            END IF;
        END IF;
    END IF;

    IF UPDATING THEN
        IF NVL(:NEW.ADVISOR_ID, -1) <> NVL(:OLD.ADVISOR_ID, -1) THEN
            raise_application_error(-20011, 'Product manager (arranger) is strictly immutable and cannot be reassigned.');
        END IF;

        IF :NEW.CLIENT_ID <> :OLD.CLIENT_ID THEN
            raise_application_error(-20012, 'A product cannot be reassigned to a different client.');
        END IF;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_EXPENSES_BIU
BEFORE INSERT OR UPDATE ON EXPENSES
FOR EACH ROW
DECLARE
    v_client_active NUMBER(1);
BEGIN
    IF INSERTING OR (UPDATING AND :NEW.CLIENT_ID <> :OLD.CLIENT_ID) THEN
        SELECT IS_ACTIVE INTO v_client_active FROM CLIENTS WHERE CLIENT_ID = :NEW.CLIENT_ID;
        IF v_client_active = 0 THEN
            raise_application_error(-20013, 'Cannot add an expense to an inactive client.');
        END IF;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_USERS_BIU
BEFORE INSERT OR UPDATE ON USERS
FOR EACH ROW
BEGIN
  IF (:NEW.USER_TYPE = 'ADVISOR' AND :NEW.ICO IS NULL) THEN
     raise_application_error(-20014, 'ICO is mandatory for advisors.');
  END IF;
END;
/

-- =============================================
-- 9. FOREIGN KEY INDEXES (Performance)
-- =============================================

CREATE INDEX IF NOT EXISTS IDX_CLIENT_USER_FK       ON CLIENTS (ADVISOR_ID);
/
CREATE INDEX IF NOT EXISTS IDX_CLIENT_RES_ADDR_FK   ON CLIENTS (RESIDENT_ADDRESS_ID);
/
CREATE INDEX IF NOT EXISTS IDX_CLIENT_CONT_ADDR_FK  ON CLIENTS (CONTACT_ADDRESS_ID);
/
CREATE INDEX IF NOT EXISTS IDX_INCOME_CLIENT_FK     ON INCOMES (CLIENT_ID);
/
CREATE INDEX IF NOT EXISTS IDX_INCOME_TYPE_FK       ON INCOMES (INCOME_TYPE_ID);
/
CREATE INDEX IF NOT EXISTS IDX_EXPENSE_CLIENT_FK    ON EXPENSES (CLIENT_ID);
/
CREATE INDEX IF NOT EXISTS IDX_EXPENSE_TYPE_FK      ON EXPENSES (EXPENSE_TYPE_ID);
/
CREATE INDEX IF NOT EXISTS IDX_PRODUCT_CLIENT_FK    ON PRODUCTS (CLIENT_ID);
/
CREATE INDEX IF NOT EXISTS IDX_PRODUCT_TYPE_FK      ON PRODUCTS (PRODUCT_TYPE_ID);
/
CREATE INDEX IF NOT EXISTS IDX_PRODUCT_PROVIDER_FK  ON PRODUCTS (PROVIDER_ID);
/
CREATE INDEX IF NOT EXISTS IDX_PRODUCT_MANAGER_FK   ON PRODUCTS (ADVISOR_ID);
/

-- =============================================
-- 10. SEARCH INDEXES (User Experience)
-- =============================================

CREATE INDEX IF NOT EXISTS IDX_USER_FIRST_LAST_CONCAT   ON USERS (LOWER(FIRST_NAME) || ' ' || LOWER(LAST_NAME));
/
CREATE UNIQUE INDEX IF NOT EXISTS IDX_ADDRESS_LOWER_UN  ON ADDRESSES (
                                                        LOWER(HOUSE_NUMBER),
                                                        LOWER(STREET),
                                                        LOWER(CITY),
                                                        LOWER(POSTAL_CODE)
);
/

-- =============================================
-- 11. ID GENERATION (SEQUENCES & AUTO-INC)
-- =============================================

CREATE SEQUENCE IF NOT EXISTS BUDGET_ITEM_SEQ START WITH 1 NOCACHE ORDER;
/

CREATE OR REPLACE TRIGGER TRG_INCOMES_ID BEFORE INSERT ON INCOMES FOR EACH ROW WHEN (NEW.INCOME_ID IS NULL)
BEGIN :NEW.INCOME_ID := BUDGET_ITEM_SEQ.NEXTVAL; END;
/

CREATE OR REPLACE TRIGGER TRG_EXPENSES_ID BEFORE INSERT ON EXPENSES FOR EACH ROW WHEN (NEW.EXPENSE_ID IS NULL)
BEGIN :NEW.EXPENSE_ID := BUDGET_ITEM_SEQ.NEXTVAL; END;
/

CREATE SEQUENCE IF NOT EXISTS ADDR_SEQ START WITH 1;
/
CREATE OR REPLACE TRIGGER TRG_ADDR_ID BEFORE INSERT ON ADDRESSES FOR EACH ROW WHEN (NEW.ADDRESS_ID IS NULL) BEGIN :NEW.ADDRESS_ID := ADDR_SEQ.NEXTVAL; END;
/

CREATE SEQUENCE IF NOT EXISTS USER_SEQ START WITH 1;
/
CREATE OR REPLACE TRIGGER TRG_USER_ID BEFORE INSERT ON USERS FOR EACH ROW WHEN (NEW.USER_ID IS NULL) BEGIN :NEW.USER_ID := USER_SEQ.NEXTVAL; END;
/

CREATE SEQUENCE IF NOT EXISTS CLIENT_SEQ START WITH 1;
/
CREATE OR REPLACE TRIGGER TRG_CLIENT_ID BEFORE INSERT ON CLIENTS FOR EACH ROW WHEN (NEW.CLIENT_ID IS NULL) BEGIN :NEW.CLIENT_ID := CLIENT_SEQ.NEXTVAL; END;
/

CREATE SEQUENCE IF NOT EXISTS INCOME_TYPE_SEQ START WITH 1;
/
CREATE OR REPLACE TRIGGER TRG_INCOME_TYPE_ID BEFORE INSERT ON INCOME_TYPES FOR EACH ROW WHEN (NEW.INCOME_TYPE_ID IS NULL) BEGIN :NEW.INCOME_TYPE_ID := INCOME_TYPE_SEQ.NEXTVAL; END;
/

CREATE SEQUENCE IF NOT EXISTS EXPENSE_TYPE_SEQ START WITH 1;
/
CREATE OR REPLACE TRIGGER TRG_EXPENSE_TYPE_ID BEFORE INSERT ON EXPENSE_TYPES FOR EACH ROW WHEN (NEW.EXPENSE_TYPE_ID IS NULL) BEGIN :NEW.EXPENSE_TYPE_ID := EXPENSE_TYPE_SEQ.NEXTVAL; END;
/

CREATE SEQUENCE IF NOT EXISTS PROVIDER_SEQ START WITH 1;
/
CREATE OR REPLACE TRIGGER TRG_PROVIDER_ID BEFORE INSERT ON PROVIDERS FOR EACH ROW WHEN (NEW.PROVIDER_ID IS NULL) BEGIN :NEW.PROVIDER_ID := PROVIDER_SEQ.NEXTVAL; END;
/

CREATE SEQUENCE IF NOT EXISTS PRODUCT_TYPE_SEQ START WITH 1;
/
CREATE OR REPLACE TRIGGER TRG_PRODUCT_TYPE_ID BEFORE INSERT ON PRODUCT_TYPES FOR EACH ROW WHEN (NEW.PRODUCT_TYPE_ID IS NULL) BEGIN :NEW.PRODUCT_TYPE_ID := PRODUCT_TYPE_SEQ.NEXTVAL; END;
/

CREATE SEQUENCE IF NOT EXISTS PRODUCT_SEQ START WITH 1;
/
CREATE OR REPLACE TRIGGER TRG_PRODUCT_ID BEFORE INSERT ON PRODUCTS FOR EACH ROW WHEN (NEW.PRODUCT_ID IS NULL) BEGIN :NEW.PRODUCT_ID := PRODUCT_SEQ.NEXTVAL; END;
/