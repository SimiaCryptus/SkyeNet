import React, {useEffect} from 'react';
import styled from 'styled-components';
import {useDispatch, useSelector} from 'react-redux';
import {RootState} from '../../store';
import {hideModal} from '../../store/slices/uiSlice';

const ModalOverlay = styled.div`
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
`;

const ModalContent = styled.div`
    background-color: ${({theme}) => theme.colors.surface};
    padding: ${({theme}) => theme.sizing.spacing.lg};
    border-radius: ${({theme}) => theme.sizing.borderRadius.md};
    min-width: 300px;
    max-width: 80vw;
    max-height: 80vh;
    min-height: 200px;
    overflow: auto;
    box-shadow: 0 4px 16px ${({theme}) => `${theme.colors.primary}20`};
    h2 {
        margin-bottom: ${({theme}) => theme.sizing.spacing.md};
        color: ${({theme}) => theme.colors.text.primary};
        font-weight: ${({theme}) => theme.typography.fontWeight.bold};
    }
    button {
        border: 1px solid ${({theme}) => theme.colors.border};
        border-radius: ${({theme}) => theme.sizing.borderRadius.sm};
        cursor: pointer;
        &:hover {
            background: ${({theme}) => theme.colors.primary};
            color: ${({theme}) => theme.colors.background};
        }
    }
`;
const LOG_PREFIX = '[Modal]';


export const Modal: React.FC = () => {
    const dispatch = useDispatch();
    const {modalOpen, modalType, modalContent} = useSelector((state: RootState) => state.ui);

    useEffect(() => {
        console.log(`${LOG_PREFIX} Modal state changed:`, {
            modalOpen,
            modalType,
            hasContent: !!modalContent,
            contentLength: modalContent?.length || 0
        });
    }, [modalOpen, modalType, modalContent]);

    if (!modalOpen) {
        console.log(`${LOG_PREFIX} Not rendering - modal is closed`);
        return null;
    }

    return (
        <ModalOverlay onClick={() => dispatch(hideModal())}>
            <ModalContent className="modal-content" onClick={e => e.stopPropagation()}>
                <h2>{modalType}</h2>
                <div dangerouslySetInnerHTML={{__html: modalContent || ''}}/>
            </ModalContent>
        </ModalOverlay>
    );
};